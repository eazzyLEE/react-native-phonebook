#import "Phonebook.h"
#import <Contacts/Contacts.h>
#import <ContactsUI/ContactsUI.h>

@interface Phonebook() <CNContactPickerDelegate>
@property (nonatomic, strong) RCTPromiseResolveBlock resolveBlock;
@property (nonatomic, strong) RCTPromiseRejectBlock rejectBlock;
@property (nonatomic, strong) RCTResponseSenderBlock callbackBlock;
@end

@implementation Phonebook

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents {
    return @[@"PhonebookContactSelected"];
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

RCT_EXPORT_METHOD(openPhonebook:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    self.resolveBlock = resolve;
    self.rejectBlock = reject;
    
    [self presentContactPickerWithOptions:options];
}

RCT_EXPORT_METHOD(openPhonebookWithCallback:(NSDictionary *)options
                  callback:(RCTResponseSenderBlock)callback) {
    
    self.callbackBlock = callback;
    
    [self presentContactPickerWithOptions:options];
}

RCT_EXPORT_METHOD(isPhonebookAvailable:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(requestContactsPermission:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    CNAuthorizationStatus status = [CNContactStore authorizationStatusForEntityType:CNEntityTypeContacts];
    
    if (status == CNAuthorizationStatusAuthorized) {
        resolve(@YES);
        return;
    }
    
    if (status == CNAuthorizationStatusDenied || status == CNAuthorizationStatusRestricted) {
        resolve(@NO);
        return;
    }
    
    CNContactStore *contactStore = [[CNContactStore alloc] init];
    [contactStore requestAccessForEntityType:CNEntityTypeContacts completionHandler:^(BOOL granted, NSError * _Nullable error) {
        resolve(@(granted));
    }];
}

RCT_EXPORT_METHOD(hasContactsPermission:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    CNAuthorizationStatus status = [CNContactStore authorizationStatusForEntityType:CNEntityTypeContacts];
    resolve(@(status == CNAuthorizationStatusAuthorized));
}

- (void)presentContactPickerWithOptions:(NSDictionary *)options {
    CNAuthorizationStatus status = [CNContactStore authorizationStatusForEntityType:CNEntityTypeContacts];
    
    if (status == CNAuthorizationStatusDenied || status == CNAuthorizationStatusRestricted) {
        [self handleError:@"Contacts permission denied"];
        return;
    }
    
    if (status == CNAuthorizationStatusNotDetermined) {
        CNContactStore *contactStore = [[CNContactStore alloc] init];
        [contactStore requestAccessForEntityType:CNEntityTypeContacts completionHandler:^(BOOL granted, NSError * _Nullable error) {
            if (granted) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self presentContactPickerWithOptions:options];
                });
            } else {
                [self handleError:@"Contacts permission denied"];
            }
        }];
        return;
    }
    
    CNContactPickerViewController *contactPicker = [[CNContactPickerViewController alloc] init];
    contactPicker.delegate = self;
    
    
    // Get the root view controller
    UIViewController *rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
    while (rootViewController.presentedViewController) {
        rootViewController = rootViewController.presentedViewController;
    }
    
    [rootViewController presentViewController:contactPicker animated:YES completion:nil];
}

- (void)contactPicker:(CNContactPickerViewController *)picker didSelectContact:(CNContact *)contact {
    [picker dismissViewControllerAnimated:YES completion:nil];
    
    NSDictionary *contactData = [self convertContactToDictionary:contact];
    NSDictionary *result = @{
        @"success": @YES,
        @"contacts": @[contactData]
    };
    
    if (self.resolveBlock) {
        self.resolveBlock(result);
        self.resolveBlock = nil;
        self.rejectBlock = nil;
    }
    
    if (self.callbackBlock) {
        self.callbackBlock(@[result]);
        self.callbackBlock = nil;
    }
}

- (void)contactPicker:(CNContactPickerViewController *)picker didSelectContacts:(NSArray<CNContact *> *)contacts {
    [picker dismissViewControllerAnimated:YES completion:nil];
    
    NSMutableArray *contactsArray = [[NSMutableArray alloc] init];
    for (CNContact *contact in contacts) {
        [contactsArray addObject:[self convertContactToDictionary:contact]];
    }
    
    NSDictionary *result = @{
        @"success": @YES,
        @"contacts": contactsArray
    };
    
    if (self.resolveBlock) {
        self.resolveBlock(result);
        self.resolveBlock = nil;
        self.rejectBlock = nil;
    }
    
    if (self.callbackBlock) {
        self.callbackBlock(@[result]);
        self.callbackBlock = nil;
    }
}

- (void)contactPickerDidCancel:(CNContactPickerViewController *)picker {
    [picker dismissViewControllerAnimated:YES completion:nil];
    
    NSDictionary *result = @{
        @"success": @NO,
        @"error": @"User cancelled contact selection"
    };
    
    if (self.resolveBlock) {
        self.resolveBlock(result);
        self.resolveBlock = nil;
        self.rejectBlock = nil;
    }
    
    if (self.callbackBlock) {
        self.callbackBlock(@[result]);
        self.callbackBlock = nil;
    }
}

- (NSDictionary *)convertContactToDictionary:(CNContact *)contact {
    NSMutableDictionary *contactDict = [[NSMutableDictionary alloc] init];
    
    contactDict[@"id"] = contact.identifier;
    contactDict[@"name"] = [CNContactFormatter stringFromContact:contact style:CNContactFormatterStyleFullName] ?: @"";
    contactDict[@"firstName"] = contact.givenName ?: @"";
    contactDict[@"lastName"] = contact.familyName ?: @"";
    contactDict[@"organization"] = contact.organizationName ?: @"";
    contactDict[@"jobTitle"] = contact.jobTitle ?: @"";
    contactDict[@"note"] = contact.note ?: @"";
    
    // Phone numbers
    NSMutableArray *phoneNumbers = [[NSMutableArray alloc] init];
    for (CNLabeledValue *phoneNumber in contact.phoneNumbers) {
        CNPhoneNumber *phone = phoneNumber.value;
        [phoneNumbers addObject:@{
            @"label": [CNLabeledValue localizedStringForLabel:phoneNumber.label] ?: @"",
            @"number": phone.stringValue ?: @""
        }];
    }
    contactDict[@"phoneNumbers"] = phoneNumbers;
    
    // Email addresses
    NSMutableArray *emails = [[NSMutableArray alloc] init];
    for (CNLabeledValue *email in contact.emailAddresses) {
        [emails addObject:@{
            @"label": [CNLabeledValue localizedStringForLabel:email.label] ?: @"",
            @"email": email.value ?: @""
        }];
    }
    contactDict[@"emails"] = emails;
    
    // Addresses
    NSMutableArray *addresses = [[NSMutableArray alloc] init];
    for (CNLabeledValue *address in contact.postalAddresses) {
        CNPostalAddress *postalAddress = address.value;
        [addresses addObject:@{
            @"label": [CNLabeledValue localizedStringForLabel:address.label] ?: @"",
            @"street": postalAddress.street ?: @"",
            @"city": postalAddress.city ?: @"",
            @"state": postalAddress.state ?: @"",
            @"postalCode": postalAddress.postalCode ?: @"",
            @"country": postalAddress.country ?: @""
        }];
    }
    contactDict[@"addresses"] = addresses;
    
    // Birthday
    if (contact.birthday) {
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        formatter.dateFormat = @"yyyy-MM-dd";
        contactDict[@"birthday"] = [formatter stringFromDate:contact.birthday.date];
    }
    
    return contactDict;
}

- (void)handleError:(NSString *)errorMessage {
    NSDictionary *result = @{
        @"success": @NO,
        @"error": errorMessage
    };
    
    if (self.resolveBlock) {
        self.resolveBlock(result);
        self.resolveBlock = nil;
        self.rejectBlock = nil;
    }
    
    if (self.callbackBlock) {
        self.callbackBlock(@[result]);
        self.callbackBlock = nil;
    }
}

@end
