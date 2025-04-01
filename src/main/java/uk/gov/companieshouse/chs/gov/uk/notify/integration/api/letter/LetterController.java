package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letter;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class LetterController {

    @PostMapping(path = "${uk.gov.companieshouse.ch.gov.uk.notify.integration.api.sendletter}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> sendLetter() {
        return ResponseEntity.status(CREATED).body("Letter sent successfully");
    }

}
