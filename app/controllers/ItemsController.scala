package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.Inject

class ItemsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.items())
  }
}
