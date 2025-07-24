/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.emcstfereferencedata.models.crdl

import play.api.libs.json.{Format, Json}

case class CodeListCode(value: String) extends AnyVal

object CodeListCode {
  given Format[CodeListCode] = Json.valueFormat[CodeListCode]
  val BC08 = CodeListCode("BC08")
  val BC11 = CodeListCode("BC11")
  val BC17 = CodeListCode("BC17")
  val BC35 = CodeListCode("BC35")
  val BC36 = CodeListCode("BC36")
  val BC37 = CodeListCode("BC37")
  val BC41 = CodeListCode("BC41")
  val BC66 = CodeListCode("BC66")
  val E200 = CodeListCode("E200")
  val BC106 = CodeListCode("BC106")
}
