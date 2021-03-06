package org.shipkit.internal.gradle.git.tasks

import spock.lang.Specification

class TokenAvailabilityMessageTest extends Specification {

    def "creates message"() {
        expect:
        TokenAvailabilityMessage.createMessage("git push", null) == "  'git push' uses GitHub write token"
        TokenAvailabilityMessage.createMessage("git push", "secret") == "  'git push' does not use GitHub write token because it was not specified"
    }
}
