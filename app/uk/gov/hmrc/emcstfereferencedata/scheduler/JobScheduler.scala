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

package uk.gov.hmrc.emcstfereferencedata.scheduler

import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.quartz.{CronScheduleBuilder, Scheduler, SchedulerFactory, Trigger}
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.scheduler.jobs.ImportReferenceDataJob
import uk.gov.hmrc.emcstfereferencedata.utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JobScheduler @Inject()(
  lifecycle: ApplicationLifecycle,
  schedulerFactory: SchedulerFactory,
  jobFactory: ScheduledJobFactory,
  config: AppConfig
)(using
  ec: ExecutionContext
) extends Logging {
  private val quartz: Scheduler = schedulerFactory.getScheduler

  private val refDataJobDetail = newJob(classOf[ImportReferenceDataJob])
    .withIdentity("import-reference-data")
    .build()

  private val refDataJobSchedule = CronScheduleBuilder
    .cronSchedule(config.importRefDataSchedule)

  private val refDataJobTrigger = newTrigger()
    .forJob(refDataJobDetail)
    .withSchedule(refDataJobSchedule)
    .build()

  private def getJobStatus(trigger: Trigger): JobStatus =
    JobStatus(quartz.getTriggerState(trigger.getKey))

  def startReferenceDataImport(): Unit = {
    quartz.triggerJob(refDataJobDetail.getKey)
  }

  def referenceDataImportStatus(): JobStatus = {
    getJobStatus(refDataJobTrigger)
  }

  private def startScheduler(): Unit = {
    val quartz = StdSchedulerFactory.getDefaultScheduler

    quartz.setJobFactory(jobFactory)

    lifecycle.addStopHook(() => Future(quartz.shutdown()))

    quartz.scheduleJob(refDataJobDetail, refDataJobTrigger)
    quartz.start()
  }

  startScheduler()
}
