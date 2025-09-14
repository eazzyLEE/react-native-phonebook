import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Alert,
  Platform,
} from 'react-native';
import Phonebook, { Contact, PhonebookResult } from 'react-native-phonebook';

const App = (): JSX.Element => {
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [permissionStatus, setPermissionStatus] = useState<string>('Unknown');

  const checkPermission = async () => {
    try {
      const hasPermission = await Phonebook.hasContactsPermission();
      setPermissionStatus(hasPermission ? 'Granted' : 'Denied');
    } catch (error) {
      setPermissionStatus('Error checking permission');
    }
  };

  const requestPermission = async () => {
    try {
      const granted = await Phonebook.requestContactsPermission();
      setPermissionStatus(granted ? 'Granted' : 'Denied');
      if (!granted) {
        Alert.alert('Permission Denied', 'Contacts permission is required to use this feature.');
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to request permission');
    }
  };

  const openPhonebook = async () => {
    try {
      const result: PhonebookResult = await Phonebook.openPhonebook({
        title: 'Select a Contact',
        message: 'Choose a contact to import',
        allowMultipleSelection: false,
      });

      if (result.success && result.contacts && result.contacts.length > 0) {
        setSelectedContact(result.contacts[0]);
      } else {
        Alert.alert('No Contact Selected', result.error || 'No contact was selected');
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to open phonebook');
    }
  };

  const openPhonebookWithCallback = () => {
    Phonebook.openPhonebookWithCallback(
      (result: PhonebookResult) => {
        if (result.success && result.contacts && result.contacts.length > 0) {
          setSelectedContact(result.contacts[0]);
        } else {
          Alert.alert('No Contact Selected', result.error || 'No contact was selected');
        }
      },
      {
        title: 'Select a Contact',
        message: 'Choose a contact to import',
        allowMultipleSelection: false,
      }
    );
  };

  const checkAvailability = async () => {
    try {
      const isAvailable = await Phonebook.isPhonebookAvailable();
      Alert.alert('Phonebook Available', isAvailable ? 'Yes' : 'No');
    } catch (error) {
      Alert.alert('Error', 'Failed to check availability');
    }
  };

  const renderContact = (contact: Contact) => (
    <View style={styles.contactContainer}>
      <Text style={styles.contactName}>{contact.name}</Text>
      {contact.phoneNumbers.length > 0 && (
        <View style={styles.contactSection}>
          <Text style={styles.sectionTitle}>Phone Numbers:</Text>
          {contact.phoneNumbers.map((phone, index) => (
            <Text key={index} style={styles.contactDetail}>
              {phone.label}: {phone.number}
            </Text>
          ))}
        </View>
      )}
      {contact.emails.length > 0 && (
        <View style={styles.contactSection}>
          <Text style={styles.sectionTitle}>Emails:</Text>
          {contact.emails.map((email, index) => (
            <Text key={index} style={styles.contactDetail}>
              {email.label}: {email.email}
            </Text>
          ))}
        </View>
      )}
      {contact.organization && (
        <Text style={styles.contactDetail}>Organization: {contact.organization}</Text>
      )}
      {contact.jobTitle && (
        <Text style={styles.contactDetail}>Job Title: {contact.jobTitle}</Text>
      )}
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#f8f9fa" />
      <ScrollView contentInsetAdjustmentBehavior="automatic" style={styles.scrollView}>
        <View style={styles.header}>
          <Text style={styles.title}>React Native Phonebook</Text>
          <Text style={styles.subtitle}>Example App</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Permission Status: {permissionStatus}</Text>
          <View style={styles.buttonRow}>
            <TouchableOpacity style={styles.button} onPress={checkPermission}>
              <Text style={styles.buttonText}>Check Permission</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.button} onPress={requestPermission}>
              <Text style={styles.buttonText}>Request Permission</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Phonebook Actions</Text>
          <TouchableOpacity style={styles.button} onPress={checkAvailability}>
            <Text style={styles.buttonText}>Check Availability</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={openPhonebook}>
            <Text style={styles.buttonText}>Open Phonebook (Promise)</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={openPhonebookWithCallback}>
            <Text style={styles.buttonText}>Open Phonebook (Callback)</Text>
          </TouchableOpacity>
        </View>

        {selectedContact && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Selected Contact:</Text>
            {renderContact(selectedContact)}
          </View>
        )}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Platform Information</Text>
          <Text style={styles.contactDetail}>Platform: {Platform.OS}</Text>
          <Text style={styles.contactDetail}>Version: {Platform.Version}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    padding: 20,
    alignItems: 'center',
    backgroundColor: '#007bff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 5,
  },
  subtitle: {
    fontSize: 16,
    color: 'white',
    opacity: 0.9,
  },
  section: {
    margin: 20,
    padding: 15,
    backgroundColor: 'white',
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  button: {
    backgroundColor: '#007bff',
    padding: 12,
    borderRadius: 8,
    marginVertical: 5,
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 5,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  contactContainer: {
    backgroundColor: '#f8f9fa',
    padding: 15,
    borderRadius: 8,
    marginTop: 10,
  },
  contactName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  contactSection: {
    marginBottom: 10,
  },
  contactDetail: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
});

export default App;
