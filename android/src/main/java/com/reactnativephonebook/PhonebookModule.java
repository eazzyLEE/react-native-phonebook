package com.reactnativephonebook;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Event;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import org.json.JSONArray;
import org.json.JSONObject;

public class PhonebookModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 1001;
    private static final int CONTACT_PICKER_REQUEST_CODE = 1002;

    private Promise promiseResolve;
    private Promise promiseReject;
    private Callback callback;

    public PhonebookModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "Phonebook";
    }

    @ReactMethod
    public void openPhonebook(ReadableMap options, Promise promise) {
        this.promiseResolve = promise;
        this.promiseReject = null;
        this.callback = null;
        
        openContactPicker(options);
    }

    @ReactMethod
    public void openPhonebookWithCallback(ReadableMap options, Callback callback) {
        this.promiseResolve = null;
        this.promiseReject = null;
        this.callback = callback;
        
        openContactPicker(options);
    }

    @ReactMethod
    public void isPhonebookAvailable(Promise promise) {
        promise.resolve(true);
    }

    @ReactMethod
    public void requestContactsPermission(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.resolve(false);
            return;
        }

        if (hasContactsPermission()) {
            promise.resolve(true);
            return;
        }

        ActivityCompat.requestPermissions(
            activity,
            new String[]{Manifest.permission.READ_CONTACTS},
            CONTACTS_PERMISSION_REQUEST_CODE
        );

        promise.resolve(hasContactsPermission());
    }

    @ReactMethod
    public void hasContactsPermission(Promise promise) {
        promise.resolve(hasContactsPermission());
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(
            getReactApplicationContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void openContactPicker(ReadableMap options) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            handleError("No current activity available");
            return;
        }

        if (!hasContactsPermission()) {
            handleError("Contacts permission not granted");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        
        try {
            activity.startActivityForResult(intent, CONTACT_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            handleError("Failed to open contact picker: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        System.out.println("PhonebookModule: onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);
        handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Not needed for contact picker
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("PhonebookModule: handleActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == CONTACT_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                System.out.println("PhonebookModule: User cancelled contact selection");
                handleResult(false, null, "User cancelled contact selection");
            } else if (resultCode == Activity.RESULT_OK && data != null) {
                System.out.println("PhonebookModule: Contact selected successfully");
                Uri contactUri = data.getData();
                if (contactUri != null) {
                    JSONObject contact = getContactDetails(contactUri);
                    if (contact != null) {
                        System.out.println("PhonebookModule: Contact details retrieved successfully");
                        handleResult(true, new JSONObject[]{contact}, null);
                    } else {
                        System.out.println("PhonebookModule: Failed to retrieve contact details");
                        handleError("Failed to retrieve contact details");
                    }
                } else {
                    System.out.println("PhonebookModule: No contact selected");
                    handleError("No contact selected");
                }
            } else {
                System.out.println("PhonebookModule: Failed to select contact");
                handleError("Failed to select contact");
            }
        } else {
            System.out.println("PhonebookModule: Request code mismatch. Expected: " + CONTACT_PICKER_REQUEST_CODE + ", Got: " + requestCode);
        }
    }

    private JSONObject getContactDetails(Uri contactUri) {
        JSONObject contact = new JSONObject();
        
        try {
            Cursor cursor = getReactApplicationContext().getContentResolver().query(
                contactUri,
                new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                },
                null,
                null,
                null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                contact.put("id", id);
                contact.put("name", name != null ? name : "");
                contact.put("firstName", "");
                contact.put("lastName", "");
                contact.put("organization", "");
                contact.put("jobTitle", "");
                contact.put("note", "");

                // Get phone numbers
                JSONArray phoneNumbers = new JSONArray();
                if (hasPhoneNumber > 0) {
                    Cursor phoneCursor = getReactApplicationContext().getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );

                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int phoneType = phoneCursor.getInt(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                            CharSequence phoneLabelCharSeq = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                getReactApplicationContext().getResources(),
                                phoneType,
                                phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
                            );
                            String phoneLabel = phoneLabelCharSeq != null ? phoneLabelCharSeq.toString() : "";

                            JSONObject phoneObj = new JSONObject();
                            phoneObj.put("label", phoneLabel != null ? phoneLabel : "");
                            phoneObj.put("number", phoneNumber != null ? phoneNumber : "");
                            phoneNumbers.put(phoneObj);
                        }
                        phoneCursor.close();
                    }
                }
                contact.put("phoneNumbers", phoneNumbers);

                // Get email addresses
                JSONArray emails = new JSONArray();
                Cursor emailCursor = getReactApplicationContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{id},
                    null
                );

                if (emailCursor != null) {
                    while (emailCursor.moveToNext()) {
                        String email = emailCursor.getString(emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        int emailType = emailCursor.getInt(emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE));
                        CharSequence emailLabelCharSeq = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                            getReactApplicationContext().getResources(),
                            emailType,
                            emailCursor.getString(emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL))
                        );
                        String emailLabel = emailLabelCharSeq != null ? emailLabelCharSeq.toString() : "";

                        JSONObject emailObj = new JSONObject();
                        emailObj.put("label", emailLabel != null ? emailLabel : "");
                        emailObj.put("email", email != null ? email : "");
                        emails.put(emailObj);
                    }
                    emailCursor.close();
                }
                contact.put("emails", emails);

                // Get organization
                Cursor orgCursor = getReactApplicationContext().getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{id, Organization.CONTENT_ITEM_TYPE},
                    null
                );

                if (orgCursor != null && orgCursor.moveToFirst()) {
                    String organization = orgCursor.getString(orgCursor.getColumnIndexOrThrow(Organization.COMPANY));
                    String jobTitle = orgCursor.getString(orgCursor.getColumnIndexOrThrow(Organization.TITLE));
                    if (organization != null && !organization.isEmpty()) {
                        contact.put("organization", organization);
                    }
                    if (jobTitle != null && !jobTitle.isEmpty()) {
                        contact.put("jobTitle", jobTitle);
                    }
                    orgCursor.close();
                }

                cursor.close();
                return contact;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void handleResult(boolean success, JSONObject[] contacts, String error) {
        JSONObject result = new JSONObject();
        try {
            result.put("success", success);
            
            if (contacts != null) {
                JSONArray contactsArray = new JSONArray();
                for (JSONObject contact : contacts) {
                    contactsArray.put(contact);
                }
                result.put("contacts", contactsArray);
            }
            
            if (error != null) {
                result.put("error", error);
            }

            WritableMap resultMap = Arguments.createMap();
            resultMap.putBoolean("success", success);
            
            if (contacts != null) {
                WritableArray contactsArray = Arguments.createArray();
                for (JSONObject contact : contacts) {
                    WritableMap contactMap = Arguments.createMap();
                    try {
                        contactMap.putString("id", contact.getString("id"));
                        contactMap.putString("name", contact.getString("name"));
                        contactMap.putString("firstName", contact.getString("firstName"));
                        contactMap.putString("lastName", contact.getString("lastName"));
                        contactMap.putString("organization", contact.getString("organization"));
                        contactMap.putString("jobTitle", contact.getString("jobTitle"));
                        contactMap.putString("note", contact.getString("note"));
                        
                        // Convert phone numbers
                        JSONArray phoneNumbers = contact.getJSONArray("phoneNumbers");
                        WritableArray phoneNumbersArray = Arguments.createArray();
                        for (int i = 0; i < phoneNumbers.length(); i++) {
                            JSONObject phoneObj = phoneNumbers.getJSONObject(i);
                            WritableMap phoneMap = Arguments.createMap();
                            phoneMap.putString("label", phoneObj.getString("label"));
                            phoneMap.putString("number", phoneObj.getString("number"));
                            phoneNumbersArray.pushMap(phoneMap);
                        }
                        contactMap.putArray("phoneNumbers", phoneNumbersArray);
                        
                        // Convert emails
                        JSONArray emails = contact.getJSONArray("emails");
                        WritableArray emailsArray = Arguments.createArray();
                        for (int i = 0; i < emails.length(); i++) {
                            JSONObject emailObj = emails.getJSONObject(i);
                            WritableMap emailMap = Arguments.createMap();
                            emailMap.putString("label", emailObj.getString("label"));
                            emailMap.putString("email", emailObj.getString("email"));
                            emailsArray.pushMap(emailMap);
                        }
                        contactMap.putArray("emails", emailsArray);
                        
                        contactsArray.pushMap(contactMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                resultMap.putArray("contacts", contactsArray);
            }
            
            if (error != null) {
                resultMap.putString("error", error);
            }

            if (promiseResolve != null) {
                promiseResolve.resolve(resultMap);
                promiseResolve = null;
                promiseReject = null;
            }

            if (callback != null) {
                callback.invoke(resultMap);
                callback = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleError(String error) {
        handleResult(false, null, error);
    }
}
