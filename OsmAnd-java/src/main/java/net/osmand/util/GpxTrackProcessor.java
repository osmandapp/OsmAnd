package net.osmand.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KMapUtils;

public class GpxTrackProcessor {

    /**
     * Обрабатывает трек: удаляет внутренние пересечения между сегментами и склеивает
     * сегменты, соединяющиеся крайними точками (при необходимости разворачивая один из сегментов).
     * Результат – новый объект Track, состоящий из непересекающихся сегментов.
     *
     * @param track исходный трек с одним или более сегментами
     * @return новый Track, содержащий обработанные сегменты
     */
    public static Track processTrack(Track track) {
        // Извлекаем исходные сегменты в виде полилиний (списков точек)
        List<TrkSegment> originalSegments = track.getSegments();
        List<List<WptPt>> polylines = new ArrayList<>();
        for (TrkSegment seg : originalSegments) {
            List<WptPt> pts = new ArrayList<>(seg.getPoints());
            if (pts.size() >= 2) {
                polylines.add(pts);
            }
        }

        // Шаг 1. Собираем все внутренние точки (исключая первую и последнюю) и их появления.
        Map<WptPt, List<int[]>> pointOccurrences = new HashMap<>();
        for (int i = 0; i < polylines.size(); i++) {
            List<WptPt> pts = polylines.get(i);
            for (int j = 1; j < pts.size() - 1; j++) {
                WptPt pt = pts.get(j);
                List<int[]> occ = pointOccurrences.get(pt);
                if (occ == null) {
                    occ = new ArrayList<>();
                    pointOccurrences.put(pt, occ);
                }
                occ.add(new int[]{i, j});
            }
        }

        // Отмечаем для удаления те внутренние точки, которые встречаются более одного раза.
        Map<Integer, Set<Integer>> removalIndices = new HashMap<>();
        for (Map.Entry<WptPt, List<int[]>> entry : pointOccurrences.entrySet()) {
            List<int[]> occList = entry.getValue();
            if (occList.size() > 1) {
                for (int[] occ : occList) {
                    int segIndex = occ[0];
                    int ptIndex = occ[1];
                    removalIndices.computeIfAbsent(segIndex, k -> new HashSet<>()).add(ptIndex);
                }
            }
        }

        // Шаг 2. Удаляем отмеченные точки из каждой полилинии, при этом разбивая их на части,
        // если точка посередине приводит к разрыву.
        List<List<WptPt>> cleanedPolylines = new ArrayList<>();
        for (int i = 0; i < polylines.size(); i++) {
            List<WptPt> pts = polylines.get(i);
            Set<Integer> toRemove = removalIndices.get(i);
            if (toRemove == null || toRemove.isEmpty()) {
                cleanedPolylines.add(pts);
            } else {
                List<List<WptPt>> splits = new ArrayList<>();
                List<WptPt> current = new ArrayList<>();
                for (int j = 0; j < pts.size(); j++) {
                    if (toRemove.contains(j)) {
                        if (current.size() >= 2) {
                            splits.add(current);
                        }
                        current = new ArrayList<>();
                    } else {
                        current.add(pts.get(j));
                    }
                }
                if (current.size() >= 2) {
                    splits.add(current);
                }
                cleanedPolylines.addAll(splits);
            }
        }

        // Шаг 3. Склеиваем полилинии, если их крайние точки совпадают.
        boolean merged = true;
        while (merged) {
            merged = false;
            outer:
            for (int i = 0; i < cleanedPolylines.size(); i++) {
                List<WptPt> poly1 = cleanedPolylines.get(i);
                for (int j = i + 1; j < cleanedPolylines.size(); j++) {
                    List<WptPt> poly2 = cleanedPolylines.get(j);
                    // Случай 1: конец poly1 совпадает с началом poly2
                    if (pointsEqual(poly1.get(poly1.size() - 1), poly2.get(0))) {
                        List<WptPt> mergedPoly = new ArrayList<>(poly1);
                        mergedPoly.addAll(poly2.subList(1, poly2.size()));
                        cleanedPolylines.remove(j);
                        cleanedPolylines.set(i, mergedPoly);
                        merged = true;
                        break outer;
                    }
                    // Случай 2: начало poly1 совпадает с концом poly2
                    else if (pointsEqual(poly1.get(0), poly2.get(poly2.size() - 1))) {
                        List<WptPt> mergedPoly = new ArrayList<>(poly2);
                        mergedPoly.addAll(poly1.subList(1, poly1.size()));
                        cleanedPolylines.remove(j);
                        cleanedPolylines.set(i, mergedPoly);
                        merged = true;
                        break outer;
                    }
                    // Случай 3: начало poly1 совпадает с началом poly2 – разворачиваем poly1
                    else if (pointsEqual(poly1.get(0), poly2.get(0))) {
                        Collections.reverse(poly1);
                        List<WptPt> mergedPoly = new ArrayList<>(poly1);
                        mergedPoly.addAll(poly2.subList(1, poly2.size()));
                        cleanedPolylines.remove(j);
                        cleanedPolylines.set(i, mergedPoly);
                        merged = true;
                        break outer;
                    }
                    // Случай 4: конец poly1 совпадает с концом poly2 – разворачиваем poly2
                    else if (pointsEqual(poly1.get(poly1.size() - 1), poly2.get(poly2.size() - 1))) {
                        Collections.reverse(poly2);
                        List<WptPt> mergedPoly = new ArrayList<>(poly1);
                        mergedPoly.addAll(poly2.subList(1, poly2.size()));
                        cleanedPolylines.remove(j);
                        cleanedPolylines.set(i, mergedPoly);
                        merged = true;
                        break outer;
                    }
                }
            }
        }

        // Шаг 4. Преобразуем полученные полилинии в сегменты и формируем новый Track.
        List<TrkSegment> resultSegments = new ArrayList<>();
        for (List<WptPt> poly : cleanedPolylines) {
            if (poly.size() >= 2) {
                TrkSegment seg = new TrkSegment();
                seg.getPoints().addAll(poly);
                seg.setGeneralSegment(false);
                resultSegments.add(seg);
            }
        }

        Track resultTrack = new Track();
        resultTrack.getSegments().addAll(resultSegments);
        return resultTrack;
    }

    /**
     * Сравнивает две точки с использованием библиотечного метода areLatLonEqual.
     */
    private static boolean pointsEqual(WptPt a, WptPt b) {
        KLatLon p1 = new KLatLon(a.getLatitude(), a.getLongitude());
        KLatLon p2 = new KLatLon(b.getLatitude(), b.getLongitude());
        return KMapUtils.areLatLonEqual(p1, p2);
    }
}
