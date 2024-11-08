import { useEffect } from 'react';
import {
  requireNativeComponent,
  ViewStyle,
  DeviceEventEmitter,
  NativeSyntheticEvent,
} from 'react-native';

type RecyclerListProps = {
  data: Array<any>;
  config: {
    columnCount?: number;
    direction: 1 | 0;
    itemSpan?: number[];
  };
  onItemPress?: (event: NativeSyntheticEvent<any>) => void;
  onItemFocusChange?: (event: any) => void;
  exitDirection: (event: any) => void;
  style: ViewStyle;
};

const ComponentName = 'RecyclerListView';

// Directly instantiate the native component with correct props
const NativeRecyclerListView =
  requireNativeComponent<RecyclerListProps>(ComponentName);

const RecyclerListView = ({
  data,
  config,
  onItemPress,
  onItemFocusChange: onFocusChange,
  exitDirection,
  style,
}: RecyclerListProps) => {
  useEffect(() => {
    const subscription = DeviceEventEmitter.addListener(
      'onItemPress',
      (event) => {
        if (onItemPress) {
          onItemPress(event);
        }
      }
    );
    const subscription2 = DeviceEventEmitter.addListener(
      'onFocusChange',
      (event) => {
        if (onFocusChange) {
          onFocusChange(event);
        }
      }
    );
    const subscription3 = DeviceEventEmitter.addListener(
      'exitDirection',
      (event) => {
        if (exitDirection) {
          exitDirection(event);
        }
      }
    );

    return () => {
      subscription.remove();
      subscription2.remove();
      subscription3.remove();
    };
  }, [onItemPress, onFocusChange]);

  return (
    <NativeRecyclerListView
      style={style}
      data={data}
      config={config}
      onItemPress={onItemPress}
      onItemFocusChange={onFocusChange}
      exitDirection={exitDirection}
    />
  );
};

export default RecyclerListView;
