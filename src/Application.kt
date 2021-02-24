package meylis.com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class User(val id: Int? = null, val name: String, val age: Int)

object Users: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 255)
    var age: Column<Int> = integer("age")

    override val primaryKey = PrimaryKey(id, name="PK_User_ID")

    fun toUser(row: ResultRow): User =
        User(
            id = row[Users.id],
            name = row[Users.name],
            age = row[Users.age]
        )
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    Database.connect("jdbc:h2:tcp://localhost/./mydb;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver", user = "sa", password = "")

    transaction {
        SchemaUtils.create(Users)

        Users.insert {
            it[Users.name] = "John"
            it[Users.age] = 36
        }
    }
    routing {

        get("/user") {
            val users = transaction {
                Users.selectAll().map { Users.toUser(it) }
            }
            call.respond(users);
        }

        get("/user/{id}") {
            val id = call.parameters["id"]!!.toInt()
            val users = transaction {
                Users.select { Users.id eq id }.map { Users.toUser(it) }
            }
            call.respond(users);
        }

        post("/user") {
            val user = call.receive<User>()
            transaction {
                Users.insert {
                    it[Users.name] = user.name
                    it[Users.age] = user.age
                }
            }
            call.respond(user)
        }

        delete("/user/{id}") {
            val id = call.parameters["id"]!!.toInt()
            val users = transaction {
                Users.deleteWhere { Users.id eq id }
            }
            call.respond(users)
        }
    }
}

