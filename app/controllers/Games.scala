/**Copyright 2013 BLStream, BLStream's Patronage Program Contributors
 * 		 http://blstream.github.com/UrbanGame/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 		 http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers

import play.api._
import play.api.mvc._

object Games extends Controller {

	def newgame = Action { implicit request =>
		Ok(Scalate("newgame").render('title -> "Urban Game - Create new game", 'request -> request))
	}

	def mygames = Action { implicit request =>
		Ok(Scalate("mygames").render('title -> "Urban Game - Create new game", 'request -> request))
	}

}