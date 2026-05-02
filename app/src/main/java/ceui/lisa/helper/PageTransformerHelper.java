package ceui.lisa.helper;

import com.ToxicBakery.viewpager.transforms.ABaseTransformer;
import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
import com.ToxicBakery.viewpager.transforms.BackgroundToForegroundTransformer;
import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.ToxicBakery.viewpager.transforms.DefaultTransformer;
import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
import com.ToxicBakery.viewpager.transforms.DrawerTransformer;
import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.ToxicBakery.viewpager.transforms.FlipVerticalTransformer;
import com.ToxicBakery.viewpager.transforms.ForegroundToBackgroundTransformer;
import com.ToxicBakery.viewpager.transforms.RotateDownTransformer;
import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.ToxicBakery.viewpager.transforms.ScaleInOutTransformer;
import com.ToxicBakery.viewpager.transforms.StackTransformer;
import com.ToxicBakery.viewpager.transforms.TabletTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomInTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutSlideTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutTransformer;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ceui.lisa.activities.Shaft;
import ceui.lisa.transformer.CubeInTransformer;

public class PageTransformerHelper {

    private final static IndexedLinkedHashMap<Integer, TransformerType> transformerMap = Stream.of(
        new TransformerType(0, DefaultTransformer.class, "Default"),
        new TransformerType(1, AccordionTransformer.class, "Accordion"),
        new TransformerType(2, BackgroundToForegroundTransformer.class, "BackgroundToForeground"),
        new TransformerType(3, ForegroundToBackgroundTransformer.class, "ForegroundToBackground"),
        new TransformerType(4, CubeInTransformer.class, "CubeIn"),
        new TransformerType(5, CubeOutTransformer.class, "CubeOut"),
        new TransformerType(6, DepthPageTransformer.class, "DepthPage"),
        new TransformerType(7, FlipHorizontalTransformer.class, "FlipHorizontal"),
        new TransformerType(8, FlipVerticalTransformer.class, "FlipVertical"),
        new TransformerType(9, RotateDownTransformer.class, "RotateDown"),
        new TransformerType(10, RotateUpTransformer.class, "RotateUp"),
        new TransformerType(11, ScaleInOutTransformer.class, "ScaleInOut"),
        new TransformerType(12, ZoomOutSlideTransformer.class, "ZoomOutSlide"),
        new TransformerType(13, ZoomInTransformer.class, "ZoomIn"),
        new TransformerType(14, ZoomOutTransformer.class, "ZoomOut"),
        new TransformerType(15, StackTransformer.class, "Stack"),
        new TransformerType(16, TabletTransformer.class, "Tablet"),
        new TransformerType(17, DrawerTransformer.class, "Drawer")
    ).collect(Collectors.toMap(TransformerType::getTypeId, t -> t, (v1, v2) -> v1, IndexedLinkedHashMap::new)).tidyIndexes();

    public static int getCurrentTransformerIndex() {
        int transformerType = Shaft.sSettings.getTransformerType();
        if (!transformerMap.containsKey(transformerType)) {
            return 0;
        }
        int index = new ArrayList<>(transformerMap.keySet()).indexOf(transformerType);
        return Math.min(Math.max(index, 0), transformerMap.size() - 1);
    }

    public static ABaseTransformer getCurrentTransformer() {
        try {
            return transformerMap.get(Shaft.sSettings.getTransformerType()).pageTransformer.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return new DefaultTransformer();
    }

    public static String[] getTransformerNames() {
        return transformerMap.values().stream().map(TransformerType::getName).toArray(String[]::new);
    }

    public static void setCurrentTransformer(int index) {
        if (index < 0 || index >= transformerMap.size()) {
            index = 0;
        }
        Shaft.sSettings.setTransformerType(transformerMap.getIndexed(index).getTypeId());
    }

    private static class TransformerType {

        private final int typeId;
        private final String name;
        private final Class<? extends ABaseTransformer> pageTransformer;

        public TransformerType(
            int typeId,
            Class<? extends ABaseTransformer> pageTransformer,
            String name
        ) {
            this.typeId = typeId;
            this.pageTransformer = pageTransformer;
            this.name = name;
        }

        public int getTypeId() {
            return typeId;
        }

        public String getName() {
            return name;
        }
    }
}
