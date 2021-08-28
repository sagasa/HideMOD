package model;

import hide.core.HidePlayerDataManager;
import hide.guns.PlayerData.ClientPlayerData;
import hide.guns.data.HideEntityDataManager;
import net.minecraft.entity.EntityLivingBase;

public interface IRenderProperty {
	float getAnimationProp(AnimationType type, float partialTicks);

	float getYaw();

	float getPitch();

	static public class SelfProp extends PlayerProp {

		@Override
		public float getAnimationProp(AnimationType type, float partialTicks) {
			if (entity != null) {
				if (type == AnimationType.ADS) {
					ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
					return data.getAds(partialTicks);
				} else if (type == AnimationType.Reload) {
					ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
					return data.getReload(partialTicks);
				}
			}

			return super.getAnimationProp(type, partialTicks);
		}
	}

	static public class PlayerProp implements IRenderProperty {

		protected EntityLivingBase entity;

		public void setEntity(EntityLivingBase entity) {
			this.entity = entity;
		}

		@Override
		public float getAnimationProp(AnimationType type, float partialTicks) {
			if (entity != null)
				switch (type) {
				case ADS:
					return HideEntityDataManager.getADSState(entity);
				case Reload:
					return HideEntityDataManager.getReloadState(entity) < 0 ? -1 : 1 - HideEntityDataManager.getReloadState(entity);
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
