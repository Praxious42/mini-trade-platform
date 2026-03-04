@system
Feature: Order integration tests

    Scenario: Market order accepted and filled
        When a market order is submitted with quantity 1 and return status code 200
        And the order is filled

    Scenario: Order failed due to margin
        When a market order is submitted with quantity 10 and errors with status code 422
        And the order fails due to insufficient margin
