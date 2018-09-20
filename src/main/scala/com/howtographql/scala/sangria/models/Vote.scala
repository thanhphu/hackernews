package com.howtographql.scala.sangria.models

import akka.http.scaladsl.model.DateTime

/**
  * @author Thanh Phu (me@thanhphu.net)
  * @since 2018.09.20
  */
case class Vote(id: Int, userId: Int, linkId: Int, createdAt: DateTime = DateTime.now)
