/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.emcstfereferencedata.utils

object StringUtils {
  def addSmartQuotes(input: String): String = {
    input
      // single quotes followed by a space, or the end of the String, or following a letter/number
      .replaceAll("(')(?=\\s|$)|(?<=[A-Za-z0-9])(')", "’")
      // leftover '
      .replaceAll("'", "‘")
  }
}
