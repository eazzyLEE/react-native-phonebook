# React Native Phonebook

A React Native library that allows you to launch the native phonebook/contacts app and retrieve selected contact details.

## Features

- 📱 Launch native contacts app on iOS and Android
- 🔐 Handle contacts permissions automatically
- 📞 Retrieve comprehensive contact information (name, phone numbers, emails, addresses, etc.)
- 🎯 Support for both Promise and Callback patterns
- 📦 TypeScript support with full type definitions
- 🔧 Easy to integrate and use

## Installation

```bash
npm install react-native-phonebook
# or
yarn add react-native-phonebook
```

### iOS Setup

1. Install pods:
```bash
cd ios && pod install
```

2. Add the following to your `ios/YourApp/Info.plist`:
```xml
<key>NSContactsUsageDescription</key>
<string>This app needs access to contacts to let you select contacts.</string>
```

### Android Setup

1. Add the following to your `android/app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

2. The library will automatically register the native module.

## Usage

### Basic Usage

```typescript
import Phonebook from 'react-native-phonebook';

// Using Promise
const selectContact = async () => {
  try {
    const result = await Phonebook.openPhonebook({
      title: 'Select a Contact',
      message: 'Choose a contact to import',
      allowMultipleSelection: false,
    });

    if (result.success && result.contacts) {
      console.log('Selected contact:', result.contacts[0]);
    } else {
      console.log('No contact selected or error:', result.error);
    }
  } catch (error) {
    console.error('Error opening phonebook:', error);
  }
};

// Using Callback
const selectContactWithCallback = () => {
  Phonebook.openPhonebookWithCallback(
    (result) => {
      if (result.success && result.contacts) {
        console.log('Selected contact:', result.contacts[0]);
      } else {
        console.log('No contact selected or error:', result.error);
      }
    },
    {
      title: 'Select a Contact',
      message: 'Choose a contact to import',
      allowMultipleSelection: false,
    }
  );
};
```

### Permission Handling

```typescript
import Phonebook from 'react-native-phonebook';

// Check if permission is granted
const checkPermission = async () => {
  const hasPermission = await Phonebook.hasContactsPermission();
  console.log('Has permission:', hasPermission);
};

// Request permission
const requestPermission = async () => {
  const granted = await Phonebook.requestContactsPermission();
  if (granted) {
    console.log('Permission granted');
  } else {
    console.log('Permission denied');
  }
};

// Check if phonebook is available
const checkAvailability = async () => {
  const isAvailable = await Phonebook.isPhonebookAvailable();
  console.log('Phonebook available:', isAvailable);
};
```

## API Reference

### Methods

#### `openPhonebook(options?: PhonebookOptions): Promise<PhonebookResult>`

Opens the native phonebook/contacts app and returns a Promise with the selected contact(s).

**Parameters:**
- `options` (optional): Configuration options for the phonebook picker

**Returns:** Promise that resolves with a `PhonebookResult` object

#### `openPhonebookWithCallback(callback: PhonebookCallback, options?: PhonebookOptions): void`

Opens the native phonebook/contacts app with a callback function.

**Parameters:**
- `callback`: Function to call when contact selection is complete
- `options` (optional): Configuration options for the phonebook picker

#### `isPhonebookAvailable(): Promise<boolean>`

Checks if the phonebook/contacts app is available on the device.

**Returns:** Promise that resolves to `true` if available, `false` otherwise

#### `requestContactsPermission(): Promise<boolean>`

Requests permission to access contacts.

**Returns:** Promise that resolves to `true` if permission granted, `false` otherwise

#### `hasContactsPermission(): Promise<boolean>`

Checks if the app has permission to access contacts.

**Returns:** Promise that resolves to `true` if permission granted, `false` otherwise

### Types

#### `PhonebookOptions`

```typescript
interface PhonebookOptions {
  title?: string;                    // Title for the contact picker
  message?: string;                  // Message for the contact picker
  allowMultipleSelection?: boolean;  // Allow selecting multiple contacts
}
```

#### `PhonebookResult`

```typescript
interface PhonebookResult {
  success: boolean;        // Whether the operation was successful
  contacts?: Contact[];    // Array of selected contacts (if any)
  error?: string;          // Error message (if any)
}
```

#### `Contact`

```typescript
interface Contact {
  id: string;                    // Unique contact identifier
  name: string;                  // Full name
  firstName?: string;            // First name
  lastName?: string;             // Last name
  phoneNumbers: PhoneNumber[];   // Array of phone numbers
  emails: Email[];              // Array of email addresses
  organization?: string;         // Organization name
  jobTitle?: string;            // Job title
  addresses: Address[];         // Array of addresses
  birthday?: string;            // Birthday (YYYY-MM-DD format)
  note?: string;                // Contact note
}
```

#### `PhoneNumber`

```typescript
interface PhoneNumber {
  label: string;    // Phone number label (e.g., "Mobile", "Home", "Work")
  number: string;   // Phone number
}
```

#### `Email`

```typescript
interface Email {
  label: string;    // Email label (e.g., "Home", "Work")
  email: string;    // Email address
}
```

#### `Address`

```typescript
interface Address {
  label: string;        // Address label (e.g., "Home", "Work")
  street?: string;      // Street address
  city?: string;        // City
  state?: string;       // State/Province
  postalCode?: string;  // Postal/ZIP code
  country?: string;     // Country
}
```

## Example

Check out the `example/` directory for a complete working example.

To run the example:

```bash
cd example
npm install
# For iOS
cd ios && pod install && cd ..
npm run ios
# For Android
npm run android
```

## Platform Differences

### iOS
- Uses `CNContactPickerViewController` from the ContactsUI framework
- Automatically handles permission requests
- Supports all contact fields including addresses, birthdays, and notes

### Android
- Uses the native Android contact picker (`Intent.ACTION_PICK`)
- Requires `READ_CONTACTS` permission
- Retrieves comprehensive contact information including phone numbers, emails, and addresses

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Troubleshooting

### Common Issues

1. **Permission denied on Android**: Make sure you've added the `READ_CONTACTS` permission to your `AndroidManifest.xml`

2. **Permission denied on iOS**: Make sure you've added the `NSContactsUsageDescription` key to your `Info.plist`

3. **Module not found**: Make sure you've run `pod install` for iOS and rebuilt your app

4. **No contacts returned**: Check if the user has granted contacts permission and if there are contacts in the device

### Getting Help

If you encounter any issues or have questions, please:

1. Check the [Issues](https://github.com/yourusername/react-native-phonebook/issues) page
2. Create a new issue with detailed information about your problem
3. Include your React Native version, platform, and error messages

## Changelog

### 1.0.0
- Initial release
- Support for iOS and Android
- Promise and callback patterns
- Full TypeScript support
- Comprehensive contact information retrieval
