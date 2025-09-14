package com.reactnativephonebook

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONArray
import org.json.JSONObject

class PhonebookModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val CONTACTS_PERMISSION_REQUEST_CODE = 1001
        private const val CONTACT_PICKER_REQUEST_CODE = 1002
    }

    private var promiseResolve: Promise? = null
    private var promiseReject: Promise? = null
    private var callback: Callback? = null

    override fun getName(): String {
        return "Phonebook"
    }

    @ReactMethod
    fun openPhonebook(options: ReadableMap, promise: Promise) {
        this.promiseResolve = promise
        this.promiseReject = null
        this.callback = null
        
        openContactPicker(options)
    }

    @ReactMethod
    fun openPhonebookWithCallback(options: ReadableMap, callback: Callback) {
        this.promiseResolve = null
        this.promiseReject = null
        this.callback = callback
        
        openContactPicker(options)
    }

    @ReactMethod
    fun isPhonebookAvailable(promise: Promise) {
        promise.resolve(true)
    }

    @ReactMethod
    fun requestContactsPermission(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.resolve(false)
            return
        }

        if (hasContactsPermission()) {
            promise.resolve(true)
            return
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )

        // Note: In a real implementation, you'd need to handle the permission result
        // This is a simplified version
        promise.resolve(hasContactsPermission())
    }

    @ReactMethod
    fun hasContactsPermission(promise: Promise) {
        promise.resolve(hasContactsPermission())
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            reactApplicationContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openContactPicker(options: ReadableMap) {
        val activity = currentActivity
        if (activity == null) {
            handleError("No current activity available")
            return
        }

        if (!hasContactsPermission()) {
            handleError("Contacts permission not granted")
            return
        }

        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        
        try {
            activity.startActivityForResult(intent, CONTACT_PICKER_REQUEST_CODE)
        } catch (e: Exception) {
            handleError("Failed to open contact picker: ${e.message}")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CONTACT_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                handleResult(false, null, "User cancelled contact selection")
            } else if (resultCode == Activity.RESULT_OK && data != null) {
                val contactUri = data.data
                if (contactUri != null) {
                    val contact = getContactDetails(contactUri)
                    if (contact != null) {
                        handleResult(true, arrayOf(contact), null)
                    } else {
                        handleError("Failed to retrieve contact details")
                    }
                } else {
                    handleError("No contact selected")
                }
            } else {
                handleError("Failed to select contact")
            }
        }
    }

    private fun getContactDetails(contactUri: Uri): JSONObject? {
        val contact = JSONObject()
        
        try {
            val cursor: Cursor? = reactApplicationContext.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                null
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhoneNumber = c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                    contact.put("id", id)
                    contact.put("name", name ?: "")
                    contact.put("firstName", "")
                    contact.put("lastName", "")
                    contact.put("organization", "")
                    contact.put("jobTitle", "")
                    contact.put("note", "")

                    // Get phone numbers
                    val phoneNumbers = JSONArray()
                    if (hasPhoneNumber) {
                        val phoneCursor: Cursor? = reactApplicationContext.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )

                        phoneCursor?.use { pc ->
                            while (pc.moveToNext()) {
                                val phoneNumber = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                val phoneType = pc.getInt(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                                val phoneLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                    reactApplicationContext.resources,
                                    phoneType,
                                    pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
                                )

                                val phoneObj = JSONObject()
                                phoneObj.put("label", phoneLabel ?: "")
                                phoneObj.put("number", phoneNumber ?: "")
                                phoneNumbers.put(phoneObj)
                            }
                        }
                    }
                    contact.put("phoneNumbers", phoneNumbers)

                    // Get email addresses
                    val emails = JSONArray()
                    val emailCursor: Cursor? = reactApplicationContext.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )

                    emailCursor?.use { ec ->
                        while (ec.moveToNext()) {
                            val email = ec.getString(ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                            val emailType = ec.getInt(ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE))
                            val emailLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                                reactApplicationContext.resources,
                                emailType,
                                ec.getString(ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL))
                            )

                            val emailObj = JSONObject()
                            emailObj.put("label", emailLabel ?: "")
                            emailObj.put("email", email ?: "")
                            emails.put(emailObj)
                        }
                    }
                    contact.put("emails", emails)

                    // Get addresses
                    val addresses = JSONArray()
                    val addressCursor: Cursor? = reactApplicationContext.contentResolver.query(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )

                    addressCursor?.use { ac ->
                        while (ac.moveToNext()) {
                            val street = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.STREET))
                            val city = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.CITY))
                            val state = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.REGION))
                            val postalCode = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE))
                            val country = ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY))
                            val addressType = ac.getInt(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE))
                            val addressLabel = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(
                                reactApplicationContext.resources,
                                addressType,
                                ac.getString(ac.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.LABEL))
                            )

                            val addressObj = JSONObject()
                            addressObj.put("label", addressLabel ?: "")
                            addressObj.put("street", street ?: "")
                            addressObj.put("city", city ?: "")
                            addressObj.put("state", state ?: "")
                            addressObj.put("postalCode", postalCode ?: "")
                            addressObj.put("country", country ?: "")
                            addresses.put(addressObj)
                        }
                    }
                    contact.put("addresses", addresses)

                    contact.put("birthday", "")

                    return contact
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun handleResult(success: Boolean, contacts: Array<JSONObject>?, error: String?) {
        val result = JSONObject()
        result.put("success", success)
        
        if (contacts != null) {
            val contactsArray = JSONArray()
            for (contact in contacts) {
                contactsArray.put(contact)
            }
            result.put("contacts", contactsArray)
        }
        
        if (error != null) {
            result.put("error", error)
        }

        val resultMap = Arguments.fromBundle(Arguments.toBundle(result))

        promiseResolve?.resolve(resultMap)
        promiseResolve = null
        promiseReject = null

        callback?.invoke(resultMap)
        callback = null
    }

    private fun handleError(error: String) {
        handleResult(false, null, error)
    }
}
