package fr.lpreaux.usermanager;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suite de tests pour exécuter tous les tests de l'application.
 * Permet de regrouper et d'organiser l'exécution des tests.
 */
@Suite
@SuiteDisplayName("User Manager Test Suite")
@SelectPackages({
        "fr.lpreaux.usermanager.domain",
        "fr.lpreaux.usermanager.application",
        "fr.lpreaux.usermanager.infrastructure"
})
public class AllTestsSuite {
    // Cette classe est juste un conteneur pour les annotations
    // JUnit Platform l'utilisera pour découvrir et exécuter les tests
}