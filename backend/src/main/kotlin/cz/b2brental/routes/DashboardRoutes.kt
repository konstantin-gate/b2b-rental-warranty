@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.routes

import cz.b2brental.services.DashboardService
import cz.b2brental.utils.requireRole
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/** Registrace trasy dashboardu */
public fun Route.dashboardRoutes(dashboardService: DashboardService) {
    route("/dashboard") {
        authenticate("auth-jwt") {
            get {
                call.requireRole("manager", "admin")
                call.respond(dashboardService.metrics())
            }
        }
    }
}
