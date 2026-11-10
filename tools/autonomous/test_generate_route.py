import copy
import json
import unittest

from generate_route import DEFAULT_PROJECT, generate


class RouteValidation(unittest.TestCase):
    def setUp(self):
        self.doc = json.loads(DEFAULT_PROJECT.read_text())

    def test_default_and_visual_changes_flow_to_java(self):
        self.assertIn("SHOOTING_DELAY_SECONDS = 3;", generate(self.doc))
        self.doc["lines"][0]["waitAfterMs"] = 4500
        self.doc["lines"][0]["controlPoints"][0]["x"] = 16
        output = generate(self.doc)
        self.assertIn("SHOOTING_DELAY_SECONDS = 4.5;", output)
        self.assertIn("p.of(16, 100, 0)", output)

    def test_rejects_silent_behavior_loss(self):
        cases = []
        d = copy.deepcopy(self.doc); d["lines"][0]["kind"] = "compound"; cases.append(d)
        d = copy.deepcopy(self.doc); d["lines"][0]["heading"] = {"type": "tangential"}; cases.append(d)
        d = copy.deepcopy(self.doc); d["lines"][1]["waitAfterMs"] = 100; cases.append(d)
        d = copy.deepcopy(self.doc); d["lines"][0]["throughPoints"] = [{"x": 20, "y": 100}]; cases.append(d)
        d = copy.deepcopy(self.doc); d["sequence"].append({"kind": "wait"}); cases.append(d)
        d = copy.deepcopy(self.doc); d["sequence"][1]["lineId"] = "morpheus-shoot"; cases.append(d)
        d = copy.deepcopy(self.doc); d["lines"][1]["heading"]["startDeg"] = 90; cases.append(d)
        for d in cases:
            with self.subTest(d=d), self.assertRaises(ValueError): generate(d)

    def test_rejects_invalid_coordinates_and_delay(self):
        for value in (float("nan"), float("inf"), -1, 150, True, "24"):
            d = copy.deepcopy(self.doc); d["startPoint"]["x"] = value
            with self.subTest(value=value), self.assertRaises(ValueError): generate(d)
        for value in (-1, float("nan"), 30001):
            d = copy.deepcopy(self.doc); d["lines"][0]["waitAfterMs"] = value
            with self.assertRaises(ValueError): generate(d)

    def test_sequence_wait_alternative_and_duplicate_rejection(self):
        d = copy.deepcopy(self.doc)
        d["sequence"].insert(1, {"kind": "wait", "durationMs": 3000})
        with self.assertRaises(ValueError): generate(d)
        d["lines"][0]["waitAfterMs"] = 0
        self.assertIn("SHOOTING_DELAY_SECONDS = 3;", generate(d))


if __name__ == "__main__": unittest.main()
