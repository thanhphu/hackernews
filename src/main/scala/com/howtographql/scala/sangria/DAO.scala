package com.howtographql.scala.sangria

import slick.jdbc.H2Profile.api._

import scala.concurrent.Future
import DBSchema.{Links, Users, Votes}
import com.howtographql.scala.sangria.models.{Link, User, Vote}
import sangria.execution.deferred.{RelationIds, SimpleRelation}

class DAO(db: Database) {
  def allLinks: Future[Seq[models.Link]] = db.run(Links.result)

  def getLinks(ids: Seq[Int]): Future[Seq[Link]] = {
    db.run(
      Links.filter(_.id inSet ids).result
    )
  }

  def getUsers(ids: Seq[Int]): Future[Seq[User]] = {
    db.run(
      Users.filter(_.id inSet ids).result
    )
  }

  def getVotes(ids: Seq[Int]): Future[Seq[Vote]] = {
    db.run(
      Votes.filter(_.id inSet ids).result
    )
  }

  def getLinksByUserIds(ids: Seq[Int]): Future[Seq[Link]] = {
    db.run(
      Links.filter(_.postedBy inSet ids).result
    )
  }

  def getVotesByUserIds(ids: Seq[Int]): Future[Seq[Vote]] = {
    db.run(
      Votes.filter(_.userId inSet ids).result
    )
  }

  def getVotesByRelationIds(relationIds: RelationIds[Vote]): Future[Seq[Vote]] = {
    db.run(
      Votes.filter { vote =>
        relationIds.rawIds.collect({
          case (SimpleRelation("byUser"), ids: Seq[_]) => ids.contains(vote.userId)
          case (SimpleRelation("byLink"), ids: Seq[_]) => ids.contains(vote.userId)
        }).foldLeft(true: Rep[Boolean])(_ || _)

      }.result
    )
  }
}
