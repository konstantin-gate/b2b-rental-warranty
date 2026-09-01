@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import cz.b2brental.db.Companies
import cz.b2brental.db.ContractItems
import cz.b2brental.db.ContractStatus
import cz.b2brental.db.DocumentType
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.Users
import cz.b2brental.domain.formatCzk
import cz.b2brental.utils.NotFoundException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Služba pro generování PDF dokumentů v paměti pomocí knihovny OpenPDF */
public class PdfService {
    /** Veřejná fasáda generátoru PDF; směruje podle typu dokumentu */
    public fun render(
        type: DocumentType,
        entityId: Long,
    ): ByteArray =
        when (type) {
            DocumentType.rental_contract -> rentalContract(entityId)
            DocumentType.acceptance_act -> acceptanceAct(entityId)
            DocumentType.return_act -> returnAct(entityId)
            DocumentType.service_report -> serviceReport(entityId)
            DocumentType.invoice -> invoice(entityId)
        }

    private fun createBaseFont(): BaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED)

    private fun generatePdf(builder: (Document, Font, Font, Font) -> Unit): ByteArray {
        val baos = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
        try {
            val writer = PdfWriter.getInstance(document, baos)
            writer.compressionLevel = 0
            document.open()
            val baseFont = createBaseFont()
            val titleFont = Font(baseFont, 16f, Font.BOLD)
            val boldFont = Font(baseFont, 11f, Font.BOLD)
            val regularFont = Font(baseFont, 10f, Font.NORMAL)
            builder(document, titleFont, boldFont, regularFont)
        } finally {
            if (document.isOpen) {
                document.close()
            }
        }
        return baos.toByteArray()
    }

    private fun Document.addCenteredTitle(
        text: String,
        titleFont: Font,
    ) {
        val pTitle = Paragraph(text, titleFont)
        pTitle.alignment = Element.ALIGN_CENTER
        pTitle.spacingAfter = 15f
        add(pTitle)
    }

    private fun Document.addSignature(
        text: String,
        regularFont: Font,
        spacingBefore: Float = 40f,
    ) {
        val pSign = Paragraph("\n\n$text", regularFont)
        pSign.spacingBefore = spacingBefore
        add(pSign)
    }

    private fun rentalContract(contractId: Long): ByteArray =
        transaction {
            val contract =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.id eq contractId }
                    .singleOrNull() ?: throw NotFoundException("Smlouva nenalezena")

            val company =
                Companies
                    .selectAll()
                    .where { Companies.id eq contract[RentalContracts.companyId].value }
                    .singleOrNull() ?: throw NotFoundException("Společnost nenalezena")

            val items =
                (ContractItems innerJoin Equipment)
                    .selectAll()
                    .where { ContractItems.contractId eq contractId }
                    .toList()

            generatePdf { doc, titleFont, boldFont, regularFont ->
                doc.addCenteredTitle("Smlouva o nájmu", titleFont)

                doc.add(Paragraph("Pronajímatel: B2B Rental & Warranty s.r.o.", regularFont))
                doc.add(Paragraph("Nájemce: ${company[Companies.name]} (IČO: ${company[Companies.inn]})", regularFont))
                doc.add(Paragraph("Sídlo nájemce: ${company[Companies.address]}", regularFont))
                doc.add(Paragraph("Doručovací adresa: ${contract[RentalContracts.deliveryAddress]}", regularFont))
                doc.add(Paragraph("Číslo smlouvy: $contractId | Doba nájmu: ${contract[RentalContracts.months]} měsíců", regularFont))
                doc.add(
                    Paragraph("Platnost: od ${contract[RentalContracts.startDate]} do ${contract[RentalContracts.endDate]}", regularFont),
                )

                val pSpace = Paragraph(" ", regularFont)
                pSpace.spacingAfter = 10f
                doc.add(pSpace)

                val table = PdfPTable(3)
                table.widthPercentage = 100f
                table.addCell(PdfPCell(Phrase("Model", boldFont)))
                table.addCell(PdfPCell(Phrase("Sériové číslo", boldFont)))
                table.addCell(PdfPCell(Phrase("Měsíční sazba", boldFont)))

                for (row in items) {
                    table.addCell(PdfPCell(Phrase(row[Equipment.model], regularFont)))
                    table.addCell(PdfPCell(Phrase(row[Equipment.serialNumber], regularFont)))
                    table.addCell(PdfPCell(Phrase(row[Equipment.monthlyRate].formatCzk(), regularFont)))
                }
                doc.add(table)

                doc.add(Paragraph(" ", regularFont))
                doc.add(Paragraph("Měsíční nájemné: ${contract[RentalContracts.monthlyAmount].formatCzk()}", boldFont))
                doc.add(Paragraph("Jistota (depozit): ${contract[RentalContracts.deposit].formatCzk()}", boldFont))
                doc.add(Paragraph("Celková částka: ${contract[RentalContracts.totalAmount].formatCzk()}", boldFont))

                doc.addSignature("Dodavatel: _______________________          Odběratel: _______________________", regularFont, 30f)
            }
        }

    private fun acceptanceAct(equipmentId: Long): ByteArray =
        transaction {
            val eq =
                Equipment
                    .selectAll()
                    .where { Equipment.id eq equipmentId }
                    .singleOrNull() ?: throw NotFoundException("Vybavení nenalezeno")

            val cat =
                EquipmentCategories
                    .selectAll()
                    .where { EquipmentCategories.id eq eq[Equipment.categoryId].value }
                    .singleOrNull()

            val activeContract =
                (ContractItems innerJoin RentalContracts)
                    .selectAll()
                    .where { (ContractItems.equipmentId eq equipmentId) and (RentalContracts.status eq ContractStatus.active) }
                    .firstOrNull()

            val companyName =
                if (activeContract != null) {
                    Companies
                        .selectAll()
                        .where { Companies.id eq activeContract[RentalContracts.companyId].value }
                        .singleOrNull()
                        ?.get(Companies.name) ?: "—"
                } else {
                    "—"
                }

            generatePdf { doc, titleFont, boldFont, regularFont ->
                doc.addCenteredTitle("Dodací list — protokol o uvedení do provozu", titleFont)

                doc.add(Paragraph("Zařízení: ${eq[Equipment.model]}", boldFont))
                doc.add(Paragraph("Sériové číslo: ${eq[Equipment.serialNumber]}", regularFont))
                doc.add(Paragraph("Kategorie: ${cat?.get(EquipmentCategories.name) ?: "—"}", regularFont))
                doc.add(Paragraph("Odběratel: $companyName", regularFont))
                doc.add(Paragraph("Datum předání: ${LocalDate.now()}", regularFont))
                doc.add(Paragraph("Stav: v provozu", regularFont))

                doc.addSignature("Předal technik: ___________________          Převzal zástupce: ___________________", regularFont)
            }
        }

    private fun returnAct(equipmentId: Long): ByteArray =
        transaction {
            val eq =
                Equipment
                    .selectAll()
                    .where { Equipment.id eq equipmentId }
                    .singleOrNull() ?: throw NotFoundException("Vybavení nenalezeno")

            generatePdf { doc, titleFont, boldFont, regularFont ->
                doc.addCenteredTitle("Protokol o vrácení", titleFont)

                doc.add(Paragraph("Zařízení: ${eq[Equipment.model]}", boldFont))
                doc.add(Paragraph("Sériové číslo: ${eq[Equipment.serialNumber]}", regularFont))
                doc.add(Paragraph("Datum vrácení: ${LocalDate.now()}", regularFont))
                doc.add(Paragraph("Stav vybavení při vrácení: _________________________________", regularFont))
                doc.add(Paragraph("Zjištěná opotřebení a poškození: ___________________________", regularFont))
                doc.add(Paragraph("Vypočtená náhrada: _________________________________________", regularFont))

                doc.addSignature("Vrátil klient: _____________________          Převzal technik: _____________________", regularFont)
            }
        }

    private fun serviceReport(ticketId: Long): ByteArray =
        transaction {
            val ticket =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.id eq ticketId }
                    .singleOrNull() ?: throw NotFoundException("Tiket nenalezen")

            val eq =
                Equipment
                    .selectAll()
                    .where { Equipment.id eq ticket[ServiceTickets.equipmentId].value }
                    .singleOrNull()

            val techEmail =
                ticket[ServiceTickets.technicianId]?.let { techId ->
                    Users
                        .selectAll()
                        .where { Users.id eq techId.value }
                        .singleOrNull()
                        ?.get(Users.email)
                } ?: "Nepřiřazeno"

            generatePdf { doc, titleFont, boldFont, regularFont ->
                doc.addCenteredTitle("Servisní zpráva", titleFont)

                doc.add(Paragraph("Číslo tiketu: $ticketId", boldFont))
                doc.add(Paragraph("Vybavení: ${eq?.get(Equipment.model)} (S/N: ${eq?.get(Equipment.serialNumber)})", regularFont))
                doc.add(Paragraph("Popis závady: ${ticket[ServiceTickets.description]}", regularFont))
                doc.add(Paragraph("AI doporučení: ${ticket[ServiceTickets.aiRecommendation] ?: "neuvedeno"}", regularFont))
                doc.add(
                    Paragraph(
                        "Záruka: ${ticket[ServiceTickets.warrantyVerdict]?.name} (${ticket[ServiceTickets.warrantyReason] ?: "—"})",
                        regularFont,
                    ),
                )
                doc.add(Paragraph("Výsledek opravy: ${ticket[ServiceTickets.resolution] ?: "—"}", boldFont))
                doc.add(Paragraph("Technik: $techEmail", regularFont))
                doc.add(
                    Paragraph(
                        "Vytvořeno: ${ticket[ServiceTickets.createdAt]} | Vyřešeno: ${ticket[ServiceTickets.resolvedAt] ?: "—"}",
                        regularFont,
                    ),
                )

                doc.addSignature("Podpis technika: ____________________          Podpis zákazníka: ____________________", regularFont)
            }
        }

    private fun invoice(paymentId: Long): ByteArray =
        transaction {
            val payment =
                Payments
                    .selectAll()
                    .where { Payments.id eq paymentId }
                    .singleOrNull() ?: throw NotFoundException("Platba nenalezena")

            val contract =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.id eq payment[Payments.contractId].value }
                    .singleOrNull() ?: throw NotFoundException("Smlouva nenalezena")

            val company =
                Companies
                    .selectAll()
                    .where { Companies.id eq contract[RentalContracts.companyId].value }
                    .singleOrNull() ?: throw NotFoundException("Společnost nenalezena")

            generatePdf { doc, titleFont, boldFont, regularFont ->
                doc.addCenteredTitle("Faktura (daňový doklad)", titleFont)

                doc.add(Paragraph("Dodavatel: B2B Rental & Warranty s.r.o.", regularFont))
                doc.add(Paragraph("Odběratel: ${company[Companies.name]} (IČO: ${company[Companies.inn]})", regularFont))
                doc.add(Paragraph("Číslo smlouvy: ${contract[RentalContracts.id].value}", regularFont))
                doc.add(Paragraph("Fakturační období: Období ${payment[Payments.period]}", regularFont))
                doc.add(Paragraph("Datum splatnosti: ${payment[Payments.dueDate]}", regularFont))
                doc.add(Paragraph("Stav platby: ${payment[Payments.status].name}", regularFont))
                doc.add(Paragraph("Částka k úhradě: ${payment[Payments.amount].formatCzk()}", boldFont))
            }
        }
}
