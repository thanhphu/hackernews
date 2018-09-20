package com.howtographql.scala.sangria.models

import akka.http.scaladsl.model.DateTime

/**
  * @author Thanh Phu (me@thanhphu.net)
  * @since 2018.09.20
  */
case class User(id: Int, name: String, email: String, password: String, createdAt: DateTime = DateTime.now)

