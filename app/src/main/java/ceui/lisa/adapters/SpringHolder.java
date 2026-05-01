package ceui.lisa.adapters;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import ceui.lisa.databinding.RecyViewHistoryBinding;

class SpringHolder extends ViewHolder<RecyViewHistoryBinding> {

    SpringAnimation spring;

    SpringHolder(RecyViewHistoryBinding bindView) {
        super(bindView);

        spring = new SpringAnimation(itemView, DynamicAnimation.TRANSLATION_X, 0);
        SpringForce force = new SpringForce(0);
        force.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        force.setStiffness(SpringForce.STIFFNESS_MEDIUM);
        spring.setSpring(force);
    }
}
