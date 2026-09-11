@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.db

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Naplnění databáze demo daty pro vývoj a demonstraci.
 * Voláno z Application.module() po vytvoření schématu.
 * Idempotentní — opakovaný běh neduplikuje data.
 */
public fun seed() {
    transaction {
        if (Users.selectAll().count() > 0) return@transaction

        // --- 1.1 Kategorie (4) ---
        val c1 =
            EquipmentCategories.insert {
                it[name] = "Chladíci komora"
                it[icon] = "cold_room"
            } get EquipmentCategories.id

        val c2 =
            EquipmentCategories.insert {
                it[name] = "Mrazící skříň"
                it[icon] = "freezer"
            } get EquipmentCategories.id

        val c3 =
            EquipmentCategories.insert {
                it[name] = "Chladíci vitrína"
                it[icon] = "display_case"
            } get EquipmentCategories.id

        val c4 =
            EquipmentCategories.insert {
                it[name] = "Klimatizační jednotka"
                it[icon] = "hvac"
            } get EquipmentCategories.id

        // --- 1.2 Vybavení (6) ---
        val e1 =
            Equipment.insert {
                it[categoryId] = c1
                it[model] = "Liebherr GKv 5790"
                it[serialNumber] = "SN-CHK-001"
                it[price] = "185000.00".toBigDecimal()
                it[monthlyRate] = "4500.00".toBigDecimal()
            } get Equipment.id

        val e2 =
            Equipment.insert {
                it[categoryId] = c1
                it[model] = "Liebherr GKv 5790"
                it[serialNumber] = "SN-CHK-002"
                it[price] = "185000.00".toBigDecimal()
                it[monthlyRate] = "4500.00".toBigDecimal()
            } get Equipment.id

        val e3 =
            Equipment.insert {
                it[categoryId] = c2
                it[model] = "Gram Eco Mid K 410"
                it[serialNumber] = "SN-MRA-001"
                it[price] = "142000.00".toBigDecimal()
                it[monthlyRate] = "3600.00".toBigDecimal()
            } get Equipment.id

        val e4 =
            Equipment.insert {
                it[categoryId] = c2
                it[model] = "Gram Eco Mid K 410"
                it[serialNumber] = "SN-MRA-002"
                it[price] = "142000.00".toBigDecimal()
                it[monthlyRate] = "3600.00".toBigDecimal()
            } get Equipment.id

        val e5 =
            Equipment.insert {
                it[categoryId] = c3
                it[model] = "Infina NG 150"
                it[serialNumber] = "SN-VIT-001"
                it[price] = "98000.00".toBigDecimal()
                it[monthlyRate] = "2500.00".toBigDecimal()
            } get Equipment.id

        val e6 =
            Equipment.insert {
                it[categoryId] = c4
                it[model] = "Daikin FTXM60A"
                it[serialNumber] = "SN-KLI-001"
                it[price] = "120000.00".toBigDecimal()
                it[monthlyRate] = "3000.00".toBigDecimal()
            } get Equipment.id

        // --- 1.3 Záruční pravidla (4) ---
        WarrantyRules.insert {
            it[categoryId] = c1
            it[warrantyMonths] = 12
            it[coverage] = "Kompletní stroj a chladicí okruh"
            it[excludedCauses] = "opotřebení, nesprávná obsluha"
        }

        WarrantyRules.insert {
            it[categoryId] = c2
            it[warrantyMonths] = 12
            it[coverage] = "Kompletní stroj a chladicí okruh"
            it[excludedCauses] = "opotřebení, nesprávná obsluha"
        }

        WarrantyRules.insert {
            it[categoryId] = c3
            it[warrantyMonths] = 18
            it[coverage] = "Sklo, osvětlení, kompresor"
            it[excludedCauses] = "opotřebení, poškození úderem"
        }

        WarrantyRules.insert {
            it[categoryId] = c4
            it[warrantyMonths] = 24
            it[coverage] = "Celá jednotka včetně dálkového ovládání"
            it[excludedCauses] = "opotřebení, filtry"
        }

        // --- 1.4 Firma (1) ---
        val companyId =
            Companies.insert {
                it[name] = "Bangkok Kitchen s.r.o."
                it[inn] = "28745001"
                it[address] = "Karlínské náměstí 7, Praha 8"
                it[creditLimit] = "50000.00".toBigDecimal()
                it[verificationStatus] = "approved"
            } get Companies.id

        // --- 1.5 Uživatelé (4) ---
        val adminId =
            Users.insert {
                it[role] = "admin"
                it[email] = "admin@b2b.demo"
                it[passwordHash] = BCrypt.hashpw("admin1234abcd", BCrypt.gensalt())
            } get Users.id

        val managerId =
            Users.insert {
                it[role] = "manager"
                it[email] = "manager@b2b.demo"
                it[passwordHash] = BCrypt.hashpw("manager1234abcd", BCrypt.gensalt())
            } get Users.id

        val techId =
            Users.insert {
                it[role] = "technician"
                it[email] = "tech@b2b.demo"
                it[passwordHash] = BCrypt.hashpw("tech12345abcd", BCrypt.gensalt())
            } get Users.id

        val clientUserId =
            Users.insert {
                it[role] = "client"
                it[email] = "kitchen@b2b.demo"
                it[passwordHash] = BCrypt.hashpw("kitchen1234abcd", BCrypt.gensalt())
                it[Users.companyId] = companyId
            } get Users.id

        // --- 1.6 Nájemní smlouva (1) ---
        val startDate = LocalDate.now().minusDays(100)
        val endDate = startDate.plusMonths(12)

        val contractId =
            RentalContracts.insert {
                it[RentalContracts.companyId] = companyId
                it[RentalContracts.startDate] = startDate
                it[RentalContracts.endDate] = endDate
                it[months] = 12
                it[monthlyAmount] = "9000.00".toBigDecimal()
                it[deposit] = "2700.00".toBigDecimal()
                it[totalAmount] = "110700.00".toBigDecimal()
                it[deliveryAddress] = "Karlínské náměstí 7, Praha 8"
                it[status] = ContractStatus.active
            } get RentalContracts.id

        // Položky smlouvy (2)
        val ci1 =
            ContractItems.insertAndGetId {
                it[ContractItems.contractId] = contractId
                it[ContractItems.equipmentId] = e1
            }

        val ci2 =
            ContractItems.insertAndGetId {
                it[ContractItems.contractId] = contractId
                it[ContractItems.equipmentId] = e2
            }

        // Aktualizace stavu vybavení zahrnutého do smlouvy
        Equipment.update({ Equipment.id inList listOf(e1, e2) }) {
            it[status] = EquipmentStatus.rented
        }

        // --- 1.7 Platby (4) ---
        val dueDate1 = startDate.plusMonths(1)
        val dueDate2 = startDate.plusMonths(2)
        val dueDate3 = startDate.plusMonths(3)
        val dueDate4 = startDate.plusMonths(4)

        Payments.insert {
            it[Payments.contractId] = contractId
            it[period] = 1
            it[amount] = "9000.00".toBigDecimal()
            it[dueDate] = dueDate1
            it[status] = PaymentStatus.paid
            it[paidAt] = dueDate1.atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        Payments.insert {
            it[Payments.contractId] = contractId
            it[period] = 2
            it[amount] = "9000.00".toBigDecimal()
            it[dueDate] = dueDate2
            it[status] = PaymentStatus.paid
            it[paidAt] = dueDate2.atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        Payments.insert {
            it[Payments.contractId] = contractId
            it[period] = 3
            it[amount] = "9000.00".toBigDecimal()
            it[dueDate] = dueDate3
            it[status] = PaymentStatus.unpaid
        }

        Payments.insert {
            it[Payments.contractId] = contractId
            it[period] = 4
            it[amount] = "9000.00".toBigDecimal()
            it[dueDate] = dueDate4
            it[status] = PaymentStatus.unpaid
        }

        // --- 1.8 Servisní tikety (3) ---
        val t1 =
            ServiceTickets.insert {
                it[ServiceTickets.equipmentId] = e5
                it[ServiceTickets.companyId] = companyId
                it[description] = "Chladnička nevychladuje, teplota 12 °C"
                it[severity] = Severity.medium
                it[warrantyVerdict] = WarrantyVerdict.covered
                it[status] = TicketStatus.new
            } get ServiceTickets.id

        val t2 =
            ServiceTickets.insert {
                it[ServiceTickets.equipmentId] = e3
                it[ServiceTickets.companyId] = companyId
                it[description] = "Kompressor se nezapíná"
                it[severity] = Severity.critical
                it[warrantyVerdict] = WarrantyVerdict.covered
                it[ServiceTickets.technicianId] = techId
                it[status] = TicketStatus.assigned
            } get ServiceTickets.id

        val t3 =
            ServiceTickets.insert {
                it[ServiceTickets.equipmentId] = e2
                it[ServiceTickets.companyId] = companyId
                it[description] = "Únik chladiva"
                it[severity] = Severity.critical
                it[warrantyVerdict] = WarrantyVerdict.covered
                it[ServiceTickets.technicianId] = techId
                it[resolution] = "repaired"
                it[status] = TicketStatus.resolved
                it[resolvedAt] =
                    LocalDate
                        .now()
                        .minusDays(5)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
            } get ServiceTickets.id

        // --- 1.9 Dokument (1) ---
        Documents.insert {
            it[type] = DocumentType.service_report
            it[entityType] = "ticket"
            it[entityId] = t3.value
        }

        // --- 1.10 Historie událostí (21 záznamů) ---
        // Firma
        HistoryEvents.insert {
            it[entityType] = "company"
            it[entityId] = companyId.value
            it[eventType] = "company_created"
            it[message] = "Seed: vytvořen záznam company #${companyId.value}"
        }

        // Uživatelé (4)
        for (uid in listOf(adminId, managerId, techId, clientUserId)) {
            HistoryEvents.insert {
                it[entityType] = "user"
                it[entityId] = uid.value
                it[eventType] = "user_created"
                it[message] = "Seed: vytvořen záznam user #${uid.value}"
            }
        }

        // Vybavení (6)
        for (eid in listOf(e1, e2, e3, e4, e5, e6)) {
            HistoryEvents.insert {
                it[entityType] = "equipment"
                it[entityId] = eid.value
                it[eventType] = "equipment_created"
                it[message] = "Seed: vytvořen záznam equipment #${eid.value}"
            }
        }

        // Smlouva (1)
        HistoryEvents.insert {
            it[entityType] = "contract"
            it[entityId] = contractId.value
            it[eventType] = "contract_created"
            it[message] = "Seed: vytvořen záznam contract #${contractId.value}"
        }

        // Položky smlouvy (2)
        HistoryEvents.insert {
            it[entityType] = "contract_item"
            it[entityId] = ci1.value
            it[eventType] = "contract_item_created"
            it[message] = "Seed: vytvořen záznam contract_item #${ci1.value}"
        }

        HistoryEvents.insert {
            it[entityType] = "contract_item"
            it[entityId] = ci2.value
            it[eventType] = "contract_item_created"
            it[message] = "Seed: vytvořen záznam contract_item #${ci2.value}"
        }

        // Platby (4)
        val paymentIds =
            Payments
                .selectAll()
                .where {
                    Payments.contractId eq contractId
                }.orderBy(Payments.period, SortOrder.ASC)
                .map { it[Payments.id] }

        for (pid in paymentIds) {
            HistoryEvents.insert {
                it[entityType] = "payment"
                it[entityId] = pid.value
                it[eventType] = "payment_created"
                it[message] = "Seed: vytvořen záznam payment #${pid.value}"
            }
        }

        // Tikety (3)
        for (tid in listOf(t1, t2, t3)) {
            HistoryEvents.insert {
                it[entityType] = "ticket"
                it[entityId] = tid.value
                it[eventType] = "ticket_created"
                it[message] = "Seed: vytvořen záznam ticket #${tid.value}"
            }
        }
    }
}
