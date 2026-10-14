package com.aegis.vault.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.aegis.vault.core.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Minimal in-process AutofillService.
 *
 * Design decision, deliberately conservative: Aegis offers autofill only while the
 * vault session is OPEN in this process (i.e. the user has already unlocked
 * Aegis recently and the auto-lock timer has not expired). While locked it offers
 * nothing, so a locked phone never leaks the existence of any credential. A fuller
 * implementation would launch an authenticate-then-fill activity; that UI is out
 * of scope for this build, and the conservative path is the safe default.
 *
 * The service only needs to be enabled once in system settings — that is exactly
 * what the Settings → Autofill row deep-links to.
 */
class AegisAutofillService : AutofillService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + job)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val locator = ServiceLocator.from(this)
        if (!locator.sessionHolder.isUnlocked) {
            callback.onSuccess(null)
            return
        }

        val context = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }
        val structure = context.structure

        scope.launch {
            val fields = collectFields(structure)
            val usernameIds = fields.first
            val passwordIds = fields.second
            if (usernameIds.isEmpty() || passwordIds.isEmpty()) {
                callback.onSuccess(null)
                return@launch
            }

            val matches = withContext(Dispatchers.IO) {
                val domainHint = domainFromStructure(structure)
                val logins = locator.dao().allLogins()
                if (domainHint != null) {
                    logins.filter { it.domain.contains(domainHint, ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            if (matches.isEmpty()) {
                callback.onSuccess(null)
                return@launch
            }

            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "Aegis")
            }

            val responseBuilder = FillResponse.Builder()
            matches.take(6).forEach { login ->
                lastFilledItemId = login.id
                val dataset = Dataset.Builder(presentation)
                    .apply {
                        for (id in usernameIds) {
                            setValue(id, AutofillValue.forText(login.username))
                        }
                        for (id in passwordIds) {
                            setValue(id, AutofillValue.forText(login.password))
                        }
                    }
                    .build()
                responseBuilder.addDataset(dataset)
            }
            callback.onSuccess(responseBuilder.build())
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Credential capture is out of scope for this build; the vault is the
        // source of truth and Aegis saves explicit user entries only.
        callback.onSuccess()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    /** The id of the item used for the most recent fill (informational). */
    private var lastFilledItemId: String? = null

    data class Fields(
        val usernameIds: List<AutofillId>,
        val passwordIds: List<AutofillId>,
    )

    private fun collectFields(structure: AssistStructure): Fields {
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val window = structure.getWindowNodeAt(i)
            val root = window.rootViewNode ?: continue
            walk(root) { node ->
                val hints = node.autofillHints
                val value = node.autofillValue
                val id = node.autofillId ?: return@walk
                val hintText = hints?.joinToString(" ") { it.toString() }?.lowercase()
                    ?: node.hint?.toString()?.lowercase() ?: ""
                when {
                    hintText.contains("password") -> passwordIds += id
                    value?.isText == true && hintText.isNotBlank() -> {
                        if (hintText.contains("user") || hintText.contains("email") || hintText.contains("login")) {
                            usernameIds += id
                        }
                    }
                }
            }
        }
        return Fields(usernameIds, passwordIds)
    }

    private fun domainFromStructure(structure: AssistStructure): String? {
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            var found: String? = null
            walk(root) { node ->
                if (found == null) {
                    val webDomain = node.webDomain
                    if (!webDomain.isNullOrBlank()) found = webDomain
                    else if (node.hint?.toString()?.contains("http") == true) {
                        found = node.hint.toString()
                    }
                }
            }
            if (found != null) return found!!.trim().removePrefix("www.").removePrefix("https://")
        }
        return null
    }

    private fun walk(node: android.app.assist.AssistStructure.ViewNode, visit: (android.app.assist.AssistStructure.ViewNode) -> Unit) {
        visit(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { walk(it, visit) }
        }
    }
}
