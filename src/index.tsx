import {
  requireNativeComponent,
  UIManager,
  Platform,
  type ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'recycler_list' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type RecyclerListProps = {
  color: string;
  style: ViewStyle;
};

const ComponentName = 'RecyclerListView';

export const RecyclerListView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<RecyclerListProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
