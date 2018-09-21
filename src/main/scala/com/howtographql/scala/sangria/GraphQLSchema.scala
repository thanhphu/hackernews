package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import sangria.schema.{Field, ListType, ObjectType}
import sangria.execution.deferred._
import sangria.schema._
import sangria.macros.derive.{AddFields, _}
import models.{Link, User, Vote}
import sangria.ast.StringValue

object GraphQLSchema {

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

  //////////////////////////////////// USERS

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    AddFields(
      Field("links", ListType(LinkType), resolve = c => linksFetcher.deferRelSeq(linkByUserRel, c.value.id)),
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByUserRel, c.value.id))
    )
  )
  implicit val userHasId: HasId[User, Int] = HasId[User, Int](_.id)

  private val usersFetcher: Fetcher[MyContext, User, User, Int] = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  //////////////////////////////////// LINKS
  // 1
  private val LinkType = deriveObjectType[Unit, Link](
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("postedBy", Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy)))
  )
  implicit val linkHasId: HasId[Link, Int] = HasId[Link, Int](_.id)

  val linkByUserRel: Relation[Link, Link, Int] = Relation[Link, Int]("byUser", l => Seq(l.postedBy))

  private val linksFetcher: Fetcher[MyContext, Link, Link, Int] = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: MyContext, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )


  //////////////////////////////////// VOTES

  lazy val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote](
    ExcludeFields("userId"),
    AddFields(Field("user", UserType, resolve = c => usersFetcher.defer(c.value.userId))),
    AddFields(Field("link", LinkType, resolve = c => linksFetcher.defer(c.value.linkId)))
  )
  implicit val voteHasId: HasId[Vote, Int] = HasId[Vote, Int](_.id)

  val voteByUserRel: Relation[Vote, Vote, Int] = Relation[Vote, Int]("byUser", v => Seq(v.userId))
  val voteByLinkRel: Relation[Vote, Vote, Int] = Relation[Vote, Int]("byLink", v => Seq(v.linkId))

  private val votesFetcher: Fetcher[MyContext, Vote, Vote, Int] = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids),
    (ctx: MyContext, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationIds(ids)
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