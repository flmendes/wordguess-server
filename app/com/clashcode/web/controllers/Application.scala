package com.clashcode.web.controllers

import play.api.mvc._
import com.clashcode.web.views
import play.api.libs.iteratee.{ Iteratee, Concurrent }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import play.api.libs.json.{ Json, JsValue }
import akka.actor.ActorRef
import actors.ActorPlayer
import actors.GameParameters

object Application extends Controller with GameParameters with WordPushing {

  protected val (out, channel) = Concurrent.broadcast[JsValue]

  def push(players: Seq[ActorPlayer]) = channel.push(
    Json.obj("players" ->
      Json.toJson(players.map(p =>
        Json.obj(
          "name" -> p.player.name,
          "lastAction" -> p.lastAction,
          "games" -> p.totalGames,
          "solved" -> p.solvedGames)))))

  def push(message: String) = channel.push(Json.obj("status" -> message))

  def index = Action { implicit request =>
    val url = routes.Application.status().webSocketURL()
    Ok(views.html.index(url))
  }
  
  def liveFeed = Action { implicit request =>
    val url = routes.Application.status().webSocketURL()
    Ok(views.html.liveFeed(url))
  }

  def status = WebSocket.using[JsValue] { request =>
    Logger.info("new listener")
    // ignore incoming websocket traffic
    val in = Iteratee.foreach[JsValue] {
      msg =>
        Logger.debug(msg.toString)
        val action = (msg \ "action").asOpt[String].getOrElse("")
    } map {
      _ => Logger.info("removed listener")
    }
    (in, out)
  }

}


