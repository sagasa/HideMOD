package model;

import hide.guns.data.HideEntityDataManager;
import net.minecraft.entity.EntityLivingBase;

public interface IRenderProperty {
	float getAnimationProp(AnimationType type);

	float getYaw();

	float getPitch();

	static public class SelfProp extends PlayerProp{
		@Override
		public float getAnimationProp(AnimationType type) {
			if(type!=AnimationType.ADS&&entity!=null)
				;
			return super.getAnimationProp(type);
		}
	}

	static public class PlayerProp implements IRenderProperty {

		protected EntityLivingBase entity;

		public void setEntity(EntityLivingBase entity) {
			this.entity = entity;
		}

		@Override
		public float getAnimationProp(AnimationType type) {
			if (entity != null)
				switch (type) {
				case ADS:
					return HideEntityDataManager.getADSState(entity);
				case Reload:
					return HideEntityDataManager.getReloadState(entity);
				default:
					break;
				}
			return 0f;
		}

		@Override
		public float getYaw() {
			return entity == null ? 0 : entity.rotationYawHead;
		}

		@Override
		public float getPitch() {
			return entity == null ? 0 : entity.rotationPitch;
		}

	}

}
