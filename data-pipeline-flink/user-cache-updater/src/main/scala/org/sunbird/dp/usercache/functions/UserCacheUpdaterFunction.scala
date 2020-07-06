package org.sunbird.dp.usercache.functions

import java.util
import java.util.Collections
import com.google.gson.Gson

import com.datastax.driver.core.Row
import com.datastax.driver.core.querybuilder.{Clause, QueryBuilder}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.slf4j.LoggerFactory
import org.sunbird.dp.core.cache.{DataCache, RedisConnect}
import org.sunbird.dp.core.job.{BaseProcessFunction, Metrics}
import org.sunbird.dp.core.util.CassandraUtil
import org.sunbird.dp.usercache.domain.Event
import org.sunbird.dp.usercache.task.UserCacheUpdaterConfig

import scala.collection.mutable
import scala.collection.JavaConverters._


class UserCacheUpdaterFunction(config: UserCacheUpdaterConfig)(implicit val mapTypeInfo: TypeInformation[Event])
  extends BaseProcessFunction[Event, Event](config) {

  private[this] val logger = LoggerFactory.getLogger(classOf[UserCacheUpdaterFunction])
  private var dataCache: DataCache = _
  private var cassandraConnect: CassandraUtil = _
  val gson = new Gson()

  override def metricsList(): List[String] = {
    List(config.dbReadSuccessCount, config.dbReadMissCount, config.userCacheHit, config.skipCount, config.successCount, config.totalEventsCount)
  }

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    dataCache = new DataCache(config, new RedisConnect(config), config.userStore, config.userFields)
    dataCache.init()
    cassandraConnect = new CassandraUtil(config.cassandraHost, config.cassandraPort)
  }

  override def close(): Unit = {
    super.close()
    dataCache.close()
  }

  override def processElement(event: Event, context: ProcessFunction[Event, Event]#Context, metrics: Metrics): Unit = {
    metrics.incCounter(config.totalEventsCount)
    Option(event.getId).map(id => {
      Option(event.getState).map(name => {
        val userData: mutable.Map[String, AnyRef] = name.toUpperCase match {
          case "CREATE" | "CREATED" => createAction(id, event, metrics)
          case "UPDATE" | "UPDATED" => updateAction(id, event, metrics)
          case _ => {
            logger.info(s"Invalid event state name either it should be(Create/Created/Update/Updated) but found $name for ${event.mid()}")
            metrics.incCounter(config.skipCount)
            mutable.Map[String, AnyRef]()
          }
        }
        if (!userData.isEmpty) {
          println("userdata: " + userData)
          val redisData = userData.filter{f => (null != f._2)}
          val data = redisData.map(f => (f._1, f._2.toString))
          println("data: " + data)
          val filteredData = mapAsJavaMap(data)
          filteredData.values().removeAll(Collections.singleton({}))
          dataCache.hmSet(id, filteredData)
          metrics.incCounter(config.successCount)
          metrics.incCounter(config.userCacheHit)
        } else {
          metrics.incCounter(config.skipCount)
        }
      }).getOrElse(metrics.incCounter(config.skipCount))
    }).getOrElse(metrics.incCounter(config.skipCount))
  }


  /**
   * When Edata.State is Create/Created then insert the user data into redis in string formate
   */
  def createAction(userId: String, event: Event, metrics: Metrics): mutable.Map[String, AnyRef] = {
    val userData: mutable.Map[String, AnyRef] = mutable.Map[String, AnyRef]()
    Option(event.getContextDataId(cDataType = "SignupType")).map(signInType => {
      if (config.userSelfSignedInTypeList.contains(signInType)) {
        userData.put(config.userSignInTypeKey, config.userSelfSignedKey)
      }
      if (config.userValidatedTypeList.contains(signInType)) {
        userData.put(config.userSignInTypeKey, config.userValidatedKey)
      }
    }).orNull
    userData
  }

  /**
   * When edata.state is update/updated then update the user metadta
   * information by reading from the cassandra
   */

  def updateAction(userId: String, event: Event, metrics: Metrics): mutable.Map[String, AnyRef] = {
//    println("event: " + event)
//    println("userid: " + userId)
    val userCacheData: mutable.Map[String, String] = dataCache.hgetAllWithRetry(userId)
    println("usercachedata: " + userCacheData)

    val custodianRootOrgId = getCustodianRootOrgId(metrics)


    Option(event.getContextDataId(cDataType = "UserRole")).map(loginType => {
      userCacheData.put(config.userLoginTypeKey, loginType)
    })
    val userMetaDataList = event.userMetaData()
    if (!userMetaDataList.isEmpty() && userCacheData.contains(config.userSignInTypeKey) && ("Anonymous" != userCacheData.get(config.userSignInTypeKey))) {

      // Get the user details from the cassandra table
      val userDetails: mutable.Map[String, AnyRef] = userCacheData.++(extractUserMetaData(readFromCassandra(
        keyspace = config.keySpace,
        table = config.userTable,
        QueryBuilder.eq("id", userId),
        metrics)
      ))
//      val locationIds: Map[String, AnyRef] = gson.fromJson(gson.toJson(userDetails), new util.LinkedHashMap[String, AnyRef]().getClass)
//      val locationIdList = locationIds.substring(1, locationIds.length - 1).split(",").toList
//      val locationIdArr = new util.ArrayList[String](locationIdList)
//    println("userDetails.get(\"locationids\") " + locationIds)
      // Fetching the location details from the cassandra table
//      val updatedUserDetails: mutable.Map[String, AnyRef] = userDetails.++(extractLocationMetaData(readFromCassandra(
//        keyspace = config.keySpace,
//        table = config.locationTable,
//        clause = QueryBuilder.in("id", userDetails.get("locationids").getOrElse(new util.ArrayList()).asInstanceOf[util.ArrayList[String]]),
//        metrics)
//      ))
      logger.info(s"User details ( $userId ) are fetched from the db's and updating the redis now.")
      userDetails
    } else {
      logger.info(s"Skipping the event update from databases since event Does not have user properties or user sigin in type is Anonymous ")
      userCacheData.asInstanceOf[mutable.Map[String,AnyRef]]
    }
  }

  def readFromCassandra(keyspace: String, table: String, clause: Clause, metrics: Metrics): util.List[Row] = {
    var rowSet: util.List[Row] = null
    val query = QueryBuilder.select.all.from(keyspace, table).where(clause).toString
    rowSet = cassandraConnect.find(query)
    if (null != rowSet && !rowSet.isEmpty) {
      metrics.incCounter(config.dbReadSuccessCount)
      rowSet
    } else {
      metrics.incCounter(config.dbReadMissCount)
      new util.ArrayList[Row]()
    }

  }

  def extractUserMetaData(userDetails: util.List[Row]): mutable.Map[String, AnyRef] = {
    val result: mutable.Map[String, AnyRef] = mutable.Map[String, AnyRef]()
    if (null != userDetails && !userDetails.isEmpty) {
      val row: Row = userDetails.get(0)
      val columnDefinitions = row.getColumnDefinitions()
      val columnCount = columnDefinitions.size
      for (i <- 0 until columnCount) {
        result.put(columnDefinitions.getName(i), row.getObject(i))
      }
    }
    result
  }

  def extractLocationMetaData(locationDetails: util.List[Row]): mutable.Map[String, AnyRef] = {
    val result: mutable.Map[String, AnyRef] = mutable.Map[String, AnyRef]()
    locationDetails.forEach((record: Row) => {
      record.getString("type").toLowerCase match {
        case config.stateKey => result.put(config.stateKey, record.getString("name"))
        case config.districtKey => result.put(config.districtKey, record.getString("name"))
      }
    })
    result
  }

  def getCustodianRootOrgId(metrics: Metrics): Unit = {
    readFromCassandra(
      keyspace = config.keySpace,
      table = config.userTable,
      QueryBuilder.eq("id", "custodianRootOrgId"),
      metrics
    )
  }
}
