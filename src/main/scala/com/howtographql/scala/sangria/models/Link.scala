package com.howtographql.scala.sangria.models

import akka.http.scaladsl.model.DateTime
import sangria.execution.deferred.HasId


/**
  * @author Thanh Phu (me@thanhphu.net)
  * @since 2018.09.11
  */
case class Link(id: Int, url: String, description: String, postedBy: Int, createdAt: DateTime)
