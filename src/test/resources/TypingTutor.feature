Feature: Typing Tutor
  A typing tutor let's me practice typing documents.

  Part of the expected behaviour here depends on the scheme
  we use for dealing with incorrect input.

  // TODO: where we backspace; both from an Error to none;
  //         and so marker goes backwards.

  Scenario: Correctly type in shown document
    Given a document to practice typing on
    When I input the correct characters
    Then the marker should advance

  Scenario: Incorrectly type in shown document
    Given a document to practice typing on
    When I input several incorrect characters
    Then the marker should indicate that incorrect characters have been input

