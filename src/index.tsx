import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import {
  requireNativeComponent,
  ViewStyle,
  DeviceEventEmitter,
  NativeSyntheticEvent,
  findNodeHandle,
  UIManager,
  View,
} from 'react-native';

export type ExitDirection = 'left' | 'right' | 'up' | 'down';

type RecyclerListProps<T> = {
  ref: React.MutableRefObject<View>;
  data: Array<T>;
  config: {
    columnCount?: number;
    direction: 1 | 0;
    itemSpan?: number[];
  };
  onItemPress?: (event: NativeSyntheticEvent<any>) => void;
  onItemFocusChange?: (event: any) => void;
  exitDirection: (direction: ExitDirection) => void;
  style: ViewStyle;
  focusableViews?: FocusableViews;
};

// Define the type for the imperative handle
type RecyclerListViewHandle = {
  requestFocus: () => void;
  clearFocus: () => void;
  setSurroundingViews: (focusableViews: FocusableViews) => void;
};

export type FocusableViews = {
  top?: number | null;
  bottom?: number | null;
  left?: number | null;
  right?: number | null;
};

const ComponentName = 'RecyclerListView';

// Directly instantiate the native component with correct props
const NativeRecyclerListView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<RecyclerListProps<any>>(ComponentName)
    : () => {
        throw new Error(
          `The native view '${ComponentName}' has not been registered.`
        );
      };

const RecyclerListView = forwardRef<
  RecyclerListViewHandle,
  RecyclerListProps<any>
>((props, ref) => {
  // Define nativeRef to reference the NativeRecyclerListView
  const nativeRef = useRef(null);

  const {
    data,
    config,
    onItemPress,
    onItemFocusChange: onFocusChange,
    exitDirection,
    style,
    focusableViews,
  } = props;

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

    // Make sure this effect runs when focusable views change
  }, [onItemPress, onFocusChange, focusableViews]);

  useEffect(() => {
    if (nativeRef.current && focusableViews) {
      handleSetSurroundingViews();
    }
  }, [focusableViews]);

  function directionToExit(direction: ExitDirection) {
    exitDirection(direction);
    nativeRef.current?.blur();
  }

  // Expose methods to parent component via ref
  useImperativeHandle(ref, () => ({
    requestFocus() {
      const nodeHandle = findNodeHandle(nativeRef.current); // Use nativeRef here
      const commands = UIManager.getViewManagerConfig(ComponentName)?.Commands;
      if (
        nodeHandle &&
        commands &&
        typeof commands['requestFocus'] === 'number'
      ) {
        UIManager.dispatchViewManagerCommand(
          nodeHandle,
          commands['requestFocus'].toString(),
          []
        );
      }
    },

    clearFocus() {
      const nodeHandle = findNodeHandle(nativeRef.current); // Use nativeRef here
      const commands = UIManager.getViewManagerConfig(ComponentName)?.Commands;
      if (
        nodeHandle &&
        commands &&
        typeof commands['clearFocus'] === 'number'
      ) {
        UIManager.dispatchViewManagerCommand(
          nodeHandle,
          commands['clearFocus'].toString(),
          []
        );
      }
    },

    setSurroundingViews(focusableViews: FocusableViews) {
      const nodeHandle = findNodeHandle(nativeRef.current);

      if (nodeHandle) {
        const { top, bottom, left, right } = focusableViews;
        console.log(`setSurroundingViews: ${nodeHandle} ${{ focusableViews }}`);
        const commands =
          UIManager.getViewManagerConfig(ComponentName)?.Commands;

        if (commands && typeof commands['setSurroundingViews'] === 'number') {
          UIManager.dispatchViewManagerCommand(
            nodeHandle,
            commands.setSurroundingViews.toString(),
            [top ?? -1, bottom ?? -1, left ?? -1, right ?? -1] // Pass view IDs or -1 if not present
          );
        } else {
          console.warn('setSurroundingViews command is not defined');
        }
      }
    },
  }));

  const handleSetSurroundingViews = () => {
    // Check whether ref is an object or a function
    if (typeof ref === 'object' && ref?.current?.setSurroundingViews) {
      // Call setSurroundingViews from ref's imperative handle
      ref.current.setSurroundingViews({
        top: focusableViews.top,
        bottom: focusableViews.bottom,
        left: focusableViews?.left,
        right: focusableViews?.right,
      });
    }
  };

  return (
    <NativeRecyclerListView
      ref={nativeRef}
      style={style}
      data={data}
      config={config}
      onItemPress={onItemPress}
      onItemFocusChange={onFocusChange}
      exitDirection={directionToExit}
      focusableViews={focusableViews}
    />
  );
});

export default RecyclerListView;
