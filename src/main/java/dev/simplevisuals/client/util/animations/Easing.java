package dev.simplevisuals.client.util.animations;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum Easing {
	LINEAR(x -> x),
	BOTH_SINE(x -> -(Math.cos(Math.PI * x) - 1) / 2),
	BOTH_CIRC(x -> x < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2),
	BOTH_CUBIC(x -> x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2),
	EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2),
	EASE_OUT_BACK(x -> 1 + 2.70158 * Math.pow(x - 1, 3) + 1.70158 * Math.pow(x - 1, 2)),
	EASE_OUT_CIRC(x -> Math.sqrt(1 - Math.pow(x - 1, 2))),
	EASE_OUT_CUBIC(x -> 1 - Math.pow(1 - x, 3)),
	SMOOTH_STEP(x -> -2 * Math.pow(x, 3) + (3 * Math.pow(x, 2))),
	//(-1, 0.2, 0.75, 1.5)
	simplevisuals(x -> 3 * Math.pow(1 - x, 2) * x * (-0.2) + 3 * (1 - x) * Math.pow(x, 2) * 1.5 + Math.pow(x, 3)),

	// Новые быстрые easing-функции
	OUT_QUAD(x -> x * (2 - x)),
	OUT_QUART(x -> 1 - Math.pow(1 - x, 4)),
	OUT_EXPO(x -> x == 1 ? 1 : 1 - Math.pow(2, -10 * x)),
	OUT_ELASTIC(x -> {
		if (x == 0) return 0.0;
		if (x == 1) return 1.0;
		double c4 = (2 * Math.PI) / 3;
		return Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
	}),
	OUT_BOUNCE(x -> {
		double n1 = 7.5625;
		double d1 = 2.75;

		if (x < 1 / d1) {
			return n1 * x * x;
		} else if (x < 2 / d1) {
			return n1 * (x -= 1.5 / d1) * x + 0.75;
		} else if (x < 2.5 / d1) {
			return n1 * (x -= 2.25 / d1) * x + 0.9375;
		} else {
			return n1 * (x -= 2.625 / d1) * x + 0.984375;
		}
	}),
	IN_OUT_BACK(x -> {
		double c1 = 1.70158;
		double c2 = c1 * 1.525;

		return x < 0.5
				? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
				: (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2;
	}),
	IN_OUT_ELASTIC(x -> {
		if (x == 0) return 0.0;
		if (x == 1) return 1.0;

		double c5 = (2 * Math.PI) / 4.5;

		return x < 0.5
				? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
				: (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1;
	}),
	FAST_OUT(x -> Math.pow(x, 0.5)),
	FAST_IN(x -> Math.pow(x, 2)),
	FAST_IN_OUT(x -> x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2),
	SPRING(x -> 1 - Math.cos(x * 4.5 * Math.PI) * Math.exp(-x * 6)),
	BOUNCE_OUT(x -> {
		if (x < 4/11.0) {
			return (121 * x * x) / 16.0;
		} else if (x < 8/11.0) {
			return (363/40.0 * x * x) - (99/10.0 * x) + 17/5.0;
		} else if (x < 9/10.0) {
			return (4356/361.0 * x * x) - (35442/1805.0 * x) + 16061/1805.0;
		} else {
			return (54/5.0 * x * x) - (513/25.0 * x) + 268/25.0;
		}
	});

	private final Function<Double, Double> function;

	public double apply(double arg) {
		return function.apply(arg);
	}

	// Вспомогательный метод для float значений
	public float apply(float arg) {
		return function.apply((double) arg).floatValue();
	}
}