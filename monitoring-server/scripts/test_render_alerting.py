import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("alerting", Path(__file__).with_name("render-alerting.py"))
alerting = importlib.util.module_from_spec(spec)
spec.loader.exec_module(alerting)


class AlertingContractTest(unittest.TestCase):
    def test_original_notifications_do_not_depend_on_analyzer(self):
        config = alerting.render("dev", False, False)
        self.assertNotIn("policies", config)
        self.assertEqual(len(config["contactPoints"]), 1)
        self.assertEqual(config["contactPoints"][0]["receivers"][0]["type"], "slack")
        self.assertTrue(all(r["notification_settings"]["receiver"] == "chaekchaek-dev-slack"
                            for r in config["groups"][0]["rules"]))

    def test_question_context_keeps_slack_and_webhook_independent(self):
        config = alerting.render("dev", True, False)
        receivers = config["contactPoints"][1]["receivers"]
        self.assertEqual([r["type"] for r in receivers], ["slack", "webhook"])
        self.assertIn("default.message", receivers[0]["settings"]["text"])
        self.assertIn(".Fingerprint", receivers[0]["settings"]["text"])
        self.assertIn(".StartsAt.Format", receivers[0]["settings"]["text"])
        self.assertEqual(receivers[1]["settings"]["authorization_scheme"], "Bearer")
        self.assertNotIn("policies", config)

    def test_bot_health_alerts_use_only_original_slack(self):
        config = alerting.render("dev", True, False)
        health_rules = [r for r in config["groups"][0]["rules"] if r["labels"]["analysis_profile"] == "disabled"]
        self.assertEqual(len(health_rules), 4)
        self.assertTrue(all(r["notification_settings"]["receiver"] == "chaekchaek-dev-slack" for r in health_rules))
        self.assertTrue(any(r["title"] == "IncidentBotDisconnected-dev" for r in health_rules))

    def test_production_rules_remain_separate_and_can_be_paused(self):
        dev = alerting.render("dev", True, False)
        prod = alerting.render("prod", True, True)
        dev_ids = {r["uid"] for r in dev["groups"][0]["rules"]}
        self.assertTrue(all(r["uid"] not in dev_ids and r["isPaused"] for r in prod["groups"][0]["rules"]))
        self.assertEqual(prod["contactPoints"][0]["receivers"][0]["settings"]["url"], "$SLACK_WEBHOOK_URL_PROD")


if __name__ == "__main__":
    unittest.main()
