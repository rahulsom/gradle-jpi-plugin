package org.jenkinsci.gradle.plugins.jpi.internal

class LicenseDataExtractor {
    private final XmlParser parser

    LicenseDataExtractor(XmlParser parser) {
        this.parser = parser
    }

    LicenseDataExtractor() {
        this(new XmlParser(false, false))
    }

    LicenseData extractFrom(Reader reader) {
        Node pom = parser.parse(reader)

        String name = pom['name'].text()
        String description = pom['description'].text()
        String url = pom['url'].text()
        NodeList licenses = pom['licenses']

        def mapped = licenses['license'].collect { Node license ->
            String licenseUrl = license['url'].text()
            String licenseName = license['name'].text()
            new License(licenseName, licenseUrl)
        }.toSet()
        new LicenseData(name, description, url, mapped)
    }
}
