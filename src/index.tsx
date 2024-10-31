import { useEffect } from 'react';
import {
  requireNativeComponent,
  ViewStyle,
  DeviceEventEmitter,
  NativeSyntheticEvent,
} from 'react-native';

type RecyclerListProps = {
  data: Array<any>;
  columns?: number;
  onItemPress?: (event: NativeSyntheticEvent<any>) => void;
  onFocusChange?: (event: any) => void;
  style: ViewStyle;
};

const ComponentName = 'RecyclerListView';

// Directly instantiate the native component with correct props
const NativeRecyclerListView =
  requireNativeComponent<RecyclerListProps>(ComponentName);

const RecyclerListView = ({
  data,
  columns,
  onItemPress,
  onFocusChange,
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

    return () => {
      subscription.remove();
      subscription2.remove();
    };
  }, [onItemPress, onFocusChange]);

  return (
    <NativeRecyclerListView
      style={style}
      data={data}
      columns={columns}
      onItemPress={onItemPress}
      onFocusChange={onFocusChange}
    />
  );
};

export default RecyclerListView;
