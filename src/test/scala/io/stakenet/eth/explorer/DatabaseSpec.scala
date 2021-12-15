package io.stakenet.eth.explorer

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.stakenet.eth.explorer.DockerPostgresService._
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Suite}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}

/**
 * Allow us to write integration tests depending in a postgres database.
 *
 * The database is launched in a docker instance using docker-it-scala library.
 *
 * When the database is started, play evolutions are automatically applied, the
 * idea is to let you write tests like this:
 * {{{
 *   class UserPostgresDALSpec extends PostgresRepositorySpec {
 *     lazy val dal = new UserPostgresDAL(database)
 *     ...
 *   }
 * }}}
 */
trait DatabaseSpec
    extends Suite
    with Matchers
    with DockerTestKit
    with DockerPostgresService
    with BeforeAndAfter
    with BeforeAndAfterAll {

  private val tables = List(
    "transactions",
    "blocks"
  )

  private implicit val pc: PatienceConfig = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  override def beforeAll(): Unit = {
    super.beforeAll()
    val _ = isContainerReady(postgresContainer).futureValue mustEqual true
  }

  before {
    clearDatabase()
  }

  lazy val database: Database = {
    val database = Databases(
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://localhost:$PostgresExposedPort/$DatabaseName",
      name = "default",
      config = Map(
        "username" -> PostgresUsername,
        "password" -> PostgresPassword
      )
    )

    Evolutions.applyEvolutions(database)

    database
  }

  protected def clearDatabase(): Unit = {
    database.withConnection { implicit conn =>
      tables.foreach { table =>
        _root_.anorm.SQL(s"""DELETE FROM $table""").execute()
      }
    }
  }
}
