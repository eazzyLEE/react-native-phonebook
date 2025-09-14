export interface Contact {
  id: string;
  name: string;
  firstName?: string;
  lastName?: string;
  phoneNumbers: PhoneNumber[];
  emails: Email[];
  organization?: string;
  jobTitle?: string;
  addresses: Address[];
  birthday?: string;
  note?: string;
}

export interface PhoneNumber {
  label: string;
  number: string;
}

export interface Email {
  label: string;
  email: string;
}

export interface Address {
  label: string;
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface PhonebookOptions {
  title?: string;
  message?: string;
  allowMultipleSelection?: boolean;
}

export interface PhonebookResult {
  success: boolean;
  contacts?: Contact[];
  error?: string;
}

export type PhonebookCallback = (result: PhonebookResult) => void;
