package me.tanglizi.se.web.controller.model

import java.time.Instant

import scala.beans.BeanProperty

case class RestResponse[A](@BeanProperty payload: A,
                           @BeanProperty success: Boolean,
                           @BeanProperty message: String,
                           @BeanProperty timestamp: Long = Instant.now().getEpochSecond) {

}

object RestResponse {
  def ok[A](payload: A) = new RestResponse[A](payload, true, "success")
  def fail[A](payload: A, message: String) = new RestResponse[A](payload, false, message)
}
