package net.osmand.test.activities;

import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;

import org.junit.Test;

import java.util.List;

class OverlapButtonsTest {

	public static void main(String[] args) {
		overlapTest();
	}

	@Test
	public static void overlapTest() {
		System.out.println("--------START--------");
		List<ButtonPositionSize> positions = MapHudOverlapTest.defaultLayoutExample();
		for (ButtonPositionSize b : positions) {
			System.out.println(b);
		}
		System.out.println("--------");
		ButtonPositionSize.computeNonOverlap(1, positions);
		for (ButtonPositionSize b : positions) {
			System.out.println(b);
		}
		System.out.println("--------END--------");
	}
}