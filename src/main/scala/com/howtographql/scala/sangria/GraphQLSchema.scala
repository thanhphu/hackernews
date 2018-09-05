package com.howtographql.scala.sangria

import sangria.schema.{Field, ListType, ObjectType}
import sangria.execution.deferred.{Fetcher, HasId, DeferredResolver}
import models._
// #
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  private val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )(HasId(_.id))

  val Resolver: DeferredResolver[MyContext] = DeferredResolver.fetchers(linksFetcher)

  // 1
  private val LinkType = ObjectType[Unit, Link](
    "Link",
    fields[Unit, Link](
      Field("id", IntType, resolve = _.value.id),
      Field("url", StringType, resolve = _.value.url),
      Field("description", StringType, resolve = _.value.description)
    )
  )

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.defer(c.arg("id"))
      ),
      Field("links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg("ids"))
      )
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}