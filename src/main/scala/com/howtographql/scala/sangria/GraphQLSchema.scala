package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import sangria.schema.{Field, ListType, ObjectType}
import sangria.execution.deferred.{DeferredResolver, Fetcher}
import sangria.schema._
import sangria.macros.derive.{ReplaceField, deriveObjectType}
import models.{Link, User, Vote}
import sangria.ast.StringValue
import sangria.execution.deferred.HasId

object GraphQLSchema {
  //////////////////////////////////// LINKS

  private val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  //beginning of the object's body:
  implicit val GraphQLDateTime: ScalarType[DateTime] = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  // 1
  private val LinkType = deriveObjectType[Unit, Link](
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  //////////////////////////////////// USERS

  val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User]()
  implicit val userHasId: HasId[User, Int] = HasId[User, Int](_.id)

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  //////////////////////////////////// VOTES

  val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote]()
  implicit val voteHasId: HasId[Vote, Int] = HasId[Vote, Int](_.id)

  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  //////////////////////////////////// COMMON STUFF

  val Resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  // This is like express, providing paths to requests
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.defer(c.arg(Id))
      ),
      Field("links",
        ListType(LinkType),
        arguments = List(Ids),
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("users",
        ListType(UserType),
        arguments = List(Ids),
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("votes",
        ListType(VoteType),
        arguments = List(Ids),
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}