import Foundation

struct APIKeyManager {
    /// Migrate API key from Secrets.plist to Keychain on first launch
    static func migrateIfNeeded() {
        // Only migrate if no key is already in Keychain for Gemini
        guard AIProviderSettings.apiKey(for: .gemini) == nil else { return }

        // Try reading from Secrets.plist
        guard let url = Bundle.main.url(forResource: "Secrets", withExtension: "plist"),
              let data = try? Data(contentsOf: url),
              let plist = try? PropertyListSerialization.propertyList(from: data, format: nil) as? [String: Any],
              let key = plist["GEMINI_API_KEY"] as? String,
              key != "YOUR_API_KEY_HERE"
        else { return }

        // Save to Keychain
        AIProviderSettings.setAPIKey(key, for: .gemini)
    }
}
