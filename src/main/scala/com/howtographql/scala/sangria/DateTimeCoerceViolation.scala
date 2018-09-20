package com.howtographql.scala.sangria

import sangria.validation.Violation

//package body:
case object DateTimeCoerceViolation extends Violation {
  override def errorMessage: String = "Error during parsing DateTime"
}