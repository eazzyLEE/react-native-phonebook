import { NativeModules, Platform } from 'react-native';
import type {
  Contact,
  PhonebookOptions,
  PhonebookResult,
  PhonebookCallback,
} from './types';

const LINKING_ERROR =
  `The package 'react-native-phonebook' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'cd ios && pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Phonebook = NativeModules.Phonebook
  ? NativeModules.Phonebook
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * Opens the native phonebook/contacts app and allows user to select contacts
 * @param options Configuration options for the phonebook picker
 * @returns Promise that resolves with the selected contact(s)
 */
export const openPhonebook = (
  options: PhonebookOptions = {}
): Promise<PhonebookResult> => {
  return Phonebook.openPhonebook(options);
};

/**
 * Simple contact picker that returns a single contact
 * @returns Promise that resolves with the selected contact or null if cancelled
 */
export const pickContact = (): Promise<Contact | null> => {
  return Phonebook.openPhonebook({}).then((result: PhonebookResult) => {
    if (result.success && result.contacts && result.contacts.length > 0) {
      return result.contacts[0];
    }
    return null;
  });
};

/**
 * Opens the native phonebook/contacts app with a callback
 * @param callback Function to call when contact selection is complete
 * @param options Configuration options for the phonebook picker
 */
export const openPhonebookWithCallback = (
  callback: PhonebookCallback,
  options: PhonebookOptions = {}
): void => {
  Phonebook.openPhonebookWithCallback(options, callback);
};

/**
 * Checks if the phonebook/contacts app is available on the device
 * @returns Promise that resolves to true if available, false otherwise
 */
export const isPhonebookAvailable = (): Promise<boolean> => {
  return Phonebook.isPhonebookAvailable();
};

/**
 * Requests permission to access contacts
 * @returns Promise that resolves to true if permission granted, false otherwise
 */
export const requestContactsPermission = (): Promise<boolean> => {
  return Phonebook.requestContactsPermission();
};

/**
 * Checks if the app has permission to access contacts
 * @returns Promise that resolves to true if permission granted, false otherwise
 */
export const hasContactsPermission = (): Promise<boolean> => {
  return Phonebook.hasContactsPermission();
};

export type {
  Contact,
  PhoneNumber,
  Email,
  Address,
  PhonebookOptions,
  PhonebookResult,
  PhonebookCallback,
} from './types';

export default {
  openPhonebook,
  pickContact,
  openPhonebookWithCallback,
  isPhonebookAvailable,
  requestContactsPermission,
  hasContactsPermission,
};
