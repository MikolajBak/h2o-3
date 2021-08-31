package hex.tree.uplift;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import water.Scope;
import water.TestUtil;
import water.exceptions.H2OModelBuilderIllegalArgumentException;
import water.fvec.Frame;
import water.fvec.TestFrameBuilder;
import water.fvec.Vec;
import water.runner.CloudSize;
import water.runner.H2ORunner;

import static org.junit.Assert.*;

@CloudSize(1)
@RunWith(H2ORunner.class)
public class UpliftDRFTest extends TestUtil {
    private static final Logger LOG = Logger.getLogger(UpliftDRFTest.class);

    @Test
    public void testBasicTrain() {
        try {
            Scope.enter();
            Frame train = Scope.track(parseTestFile("smalldata/uplift/criteo_uplift_13k.csv"));
            train.toCategoricalCol("treatment");
            train.toCategoricalCol("conversion");
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._ignored_columns = new String[]{"visit", "exposure"};
            p._treatment_column = "treatment";
            p._response_column = "conversion";
            p._seed = 0xDECAF;

            UpliftDRF udrf = new UpliftDRF(p);
            UpliftDRFModel model = udrf.trainModel().get();
            Scope.track_generic(model);
            assertNotNull(model);
        } finally {
            Scope.exit();
        }
    }

    @Test
    public void testBasicTrainAndScore() {
        try {
            Scope.enter();
            Frame train = Scope.track(parseTestFile("smalldata/uplift/criteo_uplift_13k.csv"));
            train.toCategoricalCol("treatment");
            train.toCategoricalCol("conversion");
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._ignored_columns = new String[]{"visit", "exposure"};
            p._treatment_column = "treatment";
            p._response_column = "conversion";
            p._seed = 0xDECAF;

            UpliftDRF udrf = new UpliftDRF(p);
            UpliftDRFModel model = udrf.trainModel().get();
            Scope.track_generic(model);
            assertNotNull(model);

            Frame out = model.score(train);
            Scope.track_generic(out);
            assertArrayEquals(new String[]{"uplift_predict", "p_y1_ct1", "p_y1_ct0"}, out.names());
            assertEquals(train.numRows(), out.numRows());
        } finally {
            Scope.exit();
        }
    }

    @Test(expected = H2OModelBuilderIllegalArgumentException.class)
    public void testBasicTrainErrorMissingTreatmentColumn() {
        try {
            Scope.enter();
            Frame train = new TestFrameBuilder()
                    .withColNames("C0", "C1", "treatment_not_default", "conversion")
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM, Vec.T_CAT, Vec.T_CAT)
                    .withDataForCol(0, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(1, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(2, ar("T", "C", "T", "T", "T", "C", "C", "C", "C", "C"))
                    .withDataForCol(3, ar("1", "0", "1", "0", "1", "0", "1", "0", "1", "1"))
                    .build();
            train.toCategoricalCol("treatment_not_default");
            train.toCategoricalCol("conversion");
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._response_column = "conversion";

            UpliftDRF udrf = new UpliftDRF(p);
            udrf.trainModel().get();
        } finally {
            Scope.exit();
        }
    }

    @Test(expected = H2OModelBuilderIllegalArgumentException.class)
    public void testBasicTrainErrorDoNotSupportMultipleTreatment() {
        try {
            Scope.enter();
            Frame train = new TestFrameBuilder()
                    .withColNames("C0", "C1", "treatment", "conversion")
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM, Vec.T_CAT, Vec.T_CAT)
                    .withDataForCol(0, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(1, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(2, ar("T", "C", "T", "T", "T2", "C", "C", "C", "C", "C"))
                    .withDataForCol(3, ar("1", "1", "1", "0", "1", "0", "1", "0", "1", "1"))
                    .build();
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._treatment_column = "treatment";
            p._response_column = "conversion";

            UpliftDRF udrf = new UpliftDRF(p);
            udrf.trainModel().get();
        } finally {
            Scope.exit();
        }
    }

    @Test(expected = H2OModelBuilderIllegalArgumentException.class)
    public void testBasicTrainErrorTreatmentMustBeCategorical() {
        try {
            Scope.enter();
            Frame train = new TestFrameBuilder()
                    .withColNames("C0", "C1", "treatment", "conversion")
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM, Vec.T_NUM, Vec.T_CAT)
                    .withDataForCol(0, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(1, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(2, ar(0, 1, 0, 1, 1, 0, 0, 0, 1, 0))
                    .withDataForCol(3, ar("1", "1", "1", "0", "1", "0", "1", "0", "1", "1"))
                    .build();
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._treatment_column = "treatment";
            p._response_column = "conversion";

            UpliftDRF udrf = new UpliftDRF(p);
            udrf.trainModel().get();
        } finally {
            Scope.exit();
        }
    }

    @Test(expected = H2OModelBuilderIllegalArgumentException.class)
    public void testBasicTrainErrorDoNotSupportMultinomialResponseColumn() {
        try {
            Scope.enter();
            Frame train = new TestFrameBuilder()
                    .withColNames("C0", "C1", "treatment", "conversion")
                    .withVecTypes(Vec.T_NUM, Vec.T_NUM, Vec.T_CAT, Vec.T_CAT)
                    .withDataForCol(0, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(1, ard(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
                    .withDataForCol(2, ar("T", "C", "T", "T", "T", "C", "C", "C", "C", "C"))
                    .withDataForCol(3, ar("1", "2", "1", "0", "1", "0", "1", "0", "1", "1"))
                    .build();
            UpliftDRFModel.UpliftDRFParameters p = new UpliftDRFModel.UpliftDRFParameters();
            p._train = train._key;
            p._treatment_column = "treatment";
            p._response_column = "conversion";

            UpliftDRF udrf = new UpliftDRF(p);
            udrf.trainModel().get();
        } finally {
            Scope.exit();
        }
    }
}
