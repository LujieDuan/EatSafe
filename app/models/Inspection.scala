package models

import scala.util.{Try, Success, Failure}
import anorm._
import play.api.db.DB
import play.api.Play.current

/**
 * Represents a single inspection of a location.
 *
 * @param date The date of the inspection.
 * @param inspectionType
 */
case class Inspection(date: String, inspectionType: String, reinspectionPriority: String,
    violations: Seq[Violation])

object Inspection {
  /**
   * Gets a list of inspections belonging to a given location.
   *
   * @param locationId The ID of the location we want the inspections for.
   * @returns List of inspection objects representing the inspections for that location.
   */
  def getInspections(locationId: Int): Try[Seq[Inspection]] = {
    Try {
      DB.withConnection { implicit connection =>
        val query = SQL(
           """
             SELECT id, inspection_date, inspection_type, reinspection_priority
             FROM inspection
             WHERE location_id = {locationId};
           """    
        ).on("locationId" -> locationId)
        
        // Here, we have a Seq[Try[Inspection]], since getting the violations can fail, which
        // causes the inspection to fail.
        val tryInspections = query().map (
          row =>
            Violation.getViolations(row[Int]("id")).map { violations =>
              Inspection(row[java.util.Date]("inspection_date").toString, row[String]("inspection_type"),
                row[String]("reinspection_priority"), violations)
            }
        ).toList

        // And convert that into a Seq[Inspection] (which the outside Try will wrap into a
        // Try[Seq[Inspection]])
        tryInspections.map(_.get)
      }
    }
  }
}