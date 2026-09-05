@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cz.b2brental.R
import cz.b2brental.presentation.util.ErrorType

/**
 * Mapování ErrorType na český text z res/values/strings.xml.
 * @param error typ chyby (null = null)
 * @return text chyby pro zobrazení v UI
 */
@Composable
public fun errorMessage(error: ErrorType?): String? = when (error) {
    null -> null
    ErrorType.OFFLINE -> stringResource(R.string.error_offline)
    ErrorType.UNAUTHORIZED -> stringResource(R.string.error_unauthorized)
    ErrorType.EMPTY_DATE -> stringResource(R.string.error_empty_date)
    ErrorType.EMPTY_ADDRESS -> stringResource(R.string.error_empty_address)
    ErrorType.RATES_LOAD_FAILED -> stringResource(R.string.error_rates_load_failed)
    ErrorType.CONTRACT_CREATE_FAILED -> stringResource(R.string.error_contract_create_failed)
    ErrorType.CONTRACT_LIST_LOAD_FAILED -> stringResource(R.string.error_contract_list_load_failed)
    ErrorType.CONTRACT_DETAIL_LOAD_FAILED -> stringResource(R.string.error_contract_detail_load_failed)
    ErrorType.CONTRACT_APPROVE_FAILED -> stringResource(R.string.error_contract_approve_failed)
    ErrorType.CONTRACT_REJECT_FAILED -> stringResource(R.string.error_contract_reject_failed)
    ErrorType.CONTRACT_PDF_FAILED -> stringResource(R.string.error_contract_pdf_failed)
    ErrorType.CATALOG_LOAD_FAILED -> stringResource(R.string.error_catalog_load_failed)
    ErrorType.EQUIPMENT_DETAIL_LOAD_FAILED -> stringResource(R.string.error_equipment_detail_load_failed)
    ErrorType.MY_EQUIPMENT_LOAD_FAILED -> stringResource(R.string.error_my_equipment_load_failed)
    ErrorType.PDF_NO_APP -> stringResource(R.string.pdf_viewer_no_app)
    ErrorType.PDF_DOWNLOAD_FAILED -> stringResource(R.string.error_pdf_download_failed)
    ErrorType.EMPTY_DESCRIPTION -> stringResource(R.string.error_empty_description)
    ErrorType.DESCRIPTION_TOO_SHORT -> stringResource(R.string.error_description_too_short)
    ErrorType.PHOTO_ENCODE_FAILED -> stringResource(R.string.error_photo_encode_failed)
    ErrorType.TICKET_CREATE_FAILED -> stringResource(R.string.error_ticket_create_failed)
    ErrorType.TICKETS_LOAD_FAILED -> stringResource(R.string.error_tickets_load_failed)
    ErrorType.TICKET_DETAIL_LOAD_FAILED -> stringResource(R.string.error_ticket_detail_load_failed)
    ErrorType.PAYMENTS_LOAD_FAILED -> stringResource(R.string.error_payments_load_failed)
    ErrorType.PAYMENT_PAY_FAILED -> stringResource(R.string.error_payment_pay_failed)
    ErrorType.PAYMENT_ALREADY_PAID -> stringResource(R.string.error_payment_already_paid)
    ErrorType.DASHBOARD_LOAD_FAILED -> stringResource(R.string.error_dashboard_load_failed)
    ErrorType.TECHNICIANS_LOAD_FAILED -> stringResource(R.string.error_technicians_load_failed)
    ErrorType.TICKET_ASSIGN_FAILED -> stringResource(R.string.error_ticket_assign_failed)
    ErrorType.TICKET_START_FAILED -> stringResource(R.string.error_ticket_start_failed)
    ErrorType.TICKET_RESOLVE_FAILED -> stringResource(R.string.error_ticket_resolve_failed)
    ErrorType.NOTES_EMPTY -> stringResource(R.string.error_notes_empty)
    ErrorType.AI_ASSISTANT_FAILED -> stringResource(R.string.error_ai_assistant_failed)
    ErrorType.ADMIN_CATALOG_VALIDATION -> stringResource(R.string.error_admin_catalog_validation)
    ErrorType.ADMIN_CATALOG_SAVE_FAILED -> stringResource(R.string.error_admin_catalog_save_failed)
    ErrorType.ADMIN_CATALOG_DELETE_FAILED -> stringResource(R.string.error_admin_catalog_delete_failed)
    ErrorType.ADMIN_CATALOG_LOAD_FAILED -> stringResource(R.string.error_admin_catalog_load_failed)
    ErrorType.UNKNOWN -> stringResource(R.string.error_unknown)
}
